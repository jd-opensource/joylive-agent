/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.governance.invoke.filter.route;

import com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException;
import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.time.TimeWindow;
import com.jd.live.agent.core.util.time.TimeWindowList;
import com.jd.live.agent.governance.annotation.ConditionalOnFlowControlEnabled;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.exception.CircuitBreakException;
import com.jd.live.agent.governance.exception.ErrorCause;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundListener;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreaker;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerFactory;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilterChain;
import com.jd.live.agent.governance.invoke.metadata.ServiceMetadata;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakEndpoint;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.policy.service.exception.ErrorParser;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.jd.live.agent.governance.exception.ErrorCause.cause;
import static com.jd.live.agent.governance.util.Predicates.isError;

/**
 * CircuitBreakerFilter
 *
 * @since 1.1.0
 */
@Injectable
@Extension(value = "CircuitBreakerFilter", order = RouteFilter.ORDER_CIRCUIT_BREAKER)
@ConditionalOnFlowControlEnabled
public class CircuitBreakerFilter implements RouteFilter, ExtensionInitializer {

    @Inject
    private Map<String, CircuitBreakerFactory> factories;

    @Inject
    private Map<String, ErrorParser> errorParsers;

    @Inject(nullable = true)
    private CircuitBreakerFactory defaultFactory;

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    private GovernanceConfig governanceConfig;

    private String defaultType;

    @Override
    public void initialize() {
        defaultType = governanceConfig.getServiceConfig().getCircuitBreaker().getType();
    }

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, RouteFilterChain chain) {
        ServiceMetadata metadata = invocation.getServiceMetadata();
        ServicePolicy servicePolicy = metadata.getServicePolicy();
        List<CircuitBreakPolicy> policies = servicePolicy == null ? null : servicePolicy.getCircuitBreakPolicies();
        if (null != policies && !policies.isEmpty()) {
            List<CircuitBreakPolicy> instancePolicies = null;
            List<CircuitBreaker> breakers = new ArrayList<>(policies.size());
            T request = invocation.getRequest();
            for (CircuitBreakPolicy policy : policies) {
                request.addErrorPolicy(policy);
                switch (policy.getLevel()) {
                    case SERVICE:
                        addCircuitBreaker(breakers, policy, policy.getUri());
                        break;
                    case API:
                        URI api = policy.getUri().path(metadata.getPath()).parameters(PolicyId.KEY_SERVICE_METHOD, metadata.getMethod());
                        addCircuitBreaker(breakers, policy, api);
                        break;
                    default:
                        instancePolicies = addPolicy(policy, instancePolicies);
                }
            }
            // add listener before acquire permit
            invocation.addListener(new CircuitBreakerListener(this::getCircuitBreaker, errorParsers, breakers, instancePolicies));
            // acquire service permit
            acquire(invocation, breakers);
            // filter broken instance
            filterHealthy(invocation, instancePolicies);
        }
        chain.filter(invocation);
    }

    /**
     * Adds a circuit breaker policy to the list of policies.
     *
     * @param policy   the circuit breaker policy to add
     * @param policies the list of circuit breaker policies
     * @return the updated list of circuit breaker policies
     */
    private List<CircuitBreakPolicy> addPolicy(CircuitBreakPolicy policy, List<CircuitBreakPolicy> policies) {
        if (policies == null) {
            policies = new ArrayList<>(2);
        }
        policies.add(policy);
        return policies;
    }

    /**
     * Filters healthy endpoints from the route target based on the provided circuit breaker policies.
     *
     * @param invocation the outbound invocation containing the route target
     * @param policies   the list of circuit breaker policies to apply
     */
    private <T extends OutboundRequest> void filterHealthy(OutboundInvocation<T> invocation,
                                                           List<CircuitBreakPolicy> policies) {
        if (policies != null && !policies.isEmpty()) {
            RouteTarget target = invocation.getRouteTarget();
            long now = System.currentTimeMillis();
            target.filter(endpoint -> isHealthy(endpoint, policies, now));
        }
    }

    /**
     * Checks if the given endpoint is healthy based on the provided circuit break policies and the current time.
     *
     * @param endpoint The endpoint to check.
     * @param policies The list of circuit break policies to apply.
     * @param now      The current time in milliseconds.
     * @return True if the endpoint is healthy, false otherwise.
     */
    private boolean isHealthy(Endpoint endpoint, List<CircuitBreakPolicy> policies, long now) {
        TimeWindowList windowList = null;
        CircuitBreakEndpoint cbe;
        for (CircuitBreakPolicy policy : policies) {
            cbe = policy.getEndpoint(endpoint.getId());
            if (cbe == null) {
                continue;
            } else if (cbe.isOpen()) {
                return false;
            } else if (cbe.isHalfOpen()) {
                continue;
            } else if (cbe.isRecover(policy.getRecoveryDuration())) {
                // in recover
                if (windowList == null) {
                    windowList = new TimeWindowList();
                }
                windowList.add(new TimeWindow(cbe.getLastUpdateTime(), policy.getRecoveryDuration()));
            } else {
                // Healthy nodes, if not being concurrently updated, will be deleted.
                policy.removeEndpoint(cbe);
            }
        }
        if (windowList != null) {
            TimeWindow window = windowList.max();
            if (window != null) {
                endpoint.setRecoverTime(window.getStartTime());
                endpoint.setRecoverDuration((int) window.getDuration());
            }
        }

        return true;
    }

    /**
     * Adds a circuit breaker to the list of breakers based on the given policy and URI.
     *
     * @param breakers the list of circuit breakers
     * @param policy   the circuit breaker policy
     * @param uri      the URI for which the circuit breaker is created
     */
    private void addCircuitBreaker(List<CircuitBreaker> breakers, CircuitBreakPolicy policy, URI uri) {
        CircuitBreaker breaker = getCircuitBreaker(policy, uri);
        if (null != breaker) {
            breakers.add(breaker);
        }
    }

    /**
     * Retrieves a circuit breaker for the given policy and URI.
     *
     * @param policy the circuit breaker policy.
     * @param uri the URI for the circuit breaker.
     * @return the circuit breaker, or null if no factory is found for the policy type.
     */
    private CircuitBreaker getCircuitBreaker(CircuitBreakPolicy policy, URI uri) {
        String type = policy.getRealizeType();
        if (type == null || type.isEmpty()) {
            type = defaultType;
        }
        CircuitBreakerFactory factory = type != null ? factories.get(type) : null;
        factory = factory == null ? defaultFactory : factory;
        return factory == null ? null : factory.get(policy, uri);
    }

    /**
     * Acquires permits from the list of circuit breakers for the given invocation.
     *
     * @param invocation      the outbound invocation.
     * @param circuitBreakers the list of circuit breakers.
     */
    private static void acquire(OutboundInvocation<?> invocation, List<CircuitBreaker> circuitBreakers) {
        acquire(circuitBreakers, breaker -> {
            DegradeConfig config = breaker.getPolicy().getDegradeConfig();
            if (config == null) {
                invocation.reject(FaultType.CIRCUIT_BREAK, "The traffic circuit break policy rejected the request.");
            } else {
                invocation.degrade(FaultType.CIRCUIT_BREAK, "The circuit break policy triggers a downgrade response.", config);
            }
        });
    }

    /**
     * Acquires permits from the list of circuit breakers.
     * If acquiring a permit from any circuit breaker fails, it rolls back all previously acquired permits
     * and executes the provided fallback action.
     *
     * @param circuitBreakers the list of circuit breakers
     * @param fallback        the fallback action to be executed if acquiring a permit fails, may be {@code null}
     * @return {@code true} if permits were successfully acquired from all circuit breakers, {@code false} otherwise
     */
    private static boolean acquire(List<CircuitBreaker> circuitBreakers, Consumer<CircuitBreaker> fallback) {
        int acquires = 0;
        for (CircuitBreaker circuitBreaker : circuitBreakers) {
            if (!circuitBreaker.acquire()) {
                if (acquires > 0) {
                    // rollback
                    int rollbacks = 0;
                    for (CircuitBreaker breaker : circuitBreakers) {
                        if (rollbacks++ < acquires) {
                            breaker.release();
                        } else {
                            break;
                        }
                    }
                }
                if (fallback != null) {
                    fallback.accept(circuitBreaker);
                }
                return false;
            }
            acquires++;
        }
        return true;
    }

    /**
     * A listener that handles circuit breaker events for outbound invocations.
     */
    private static class CircuitBreakerListener implements OutboundListener {

        private final CircuitBreakerFactory factory;

        private final Map<String, ErrorParser> errorParsers;

        private List<CircuitBreaker> circuitBreakers;

        private final List<CircuitBreakPolicy> policies;

        private final int index;

        CircuitBreakerListener(CircuitBreakerFactory factory,
                               Map<String, ErrorParser> errorParsers,
                               List<CircuitBreaker> circuitBreakers,
                               List<CircuitBreakPolicy> policies) {
            this.factory = factory;
            this.errorParsers = errorParsers;
            this.circuitBreakers = circuitBreakers;
            this.policies = policies;
            this.index = circuitBreakers.size();
        }

        @Override
        public boolean onElect(Endpoint endpoint, OutboundInvocation<?> invocation) {
            if (endpoint != null && policies != null && !policies.isEmpty()) {
                for (CircuitBreakPolicy policy : policies) {
                    URI uri = policy.getUri().parameter(PolicyId.KEY_SERVICE_ENDPOINT, endpoint.getId());
                    // The circuit breaker, if in a healthy state and not accessed for 1 minute, will be recycled.
                    CircuitBreaker breaker = factory.get(policy, uri);
                    if (breaker != null) {
                        circuitBreakers.add(breaker);
                    }
                }
                int size = circuitBreakers.size();
                if (size > index) {
                    if (!acquire(circuitBreakers.subList(index, size), null)) {
                        circuitBreakers = circuitBreakers.subList(0, index);
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public void onSuccess(Endpoint endpoint, OutboundInvocation<?> invocation, ServiceResponse response) {
            OutboundRequest request = invocation.getRequest();
            long duration = request.getDuration();
            for (CircuitBreaker circuitBreaker : circuitBreakers) {
                if (response != null && isError(circuitBreaker.getPolicy(), request, response, null, errorParsers::get)) {
                    circuitBreaker.onError(duration, new CircuitBreakException("Exception of fuse response code"));
                } else {
                    circuitBreaker.onSuccess(duration);
                }
            }
        }

        @Override
        public void onFailure(Endpoint endpoint, OutboundInvocation<?> invocation, Throwable throwable) {
            if (!(throwable instanceof RejectCircuitBreakException)) {
                OutboundRequest request = invocation.getRequest();
                long duration = request.getDuration();
                ErrorCause cause = cause(throwable, request.getErrorFunction(), null);
                for (CircuitBreaker circuitBreaker : circuitBreakers) {
                    if (cause != null && cause.match(circuitBreaker.getPolicy())) {
                        circuitBreaker.onError(duration, cause.getCause());
                    } else {
                        circuitBreaker.onSuccess(duration);
                    }
                }
            }
        }
    }

}
