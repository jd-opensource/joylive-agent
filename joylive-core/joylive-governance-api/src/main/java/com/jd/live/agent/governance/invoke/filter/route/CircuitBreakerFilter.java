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
import com.jd.live.agent.governance.invoke.permission.Licensee;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.circuitbreak.*;
import com.jd.live.agent.governance.policy.service.exception.ErrorParser;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
                        URI api = policy.getUri().path(metadata.getPath()).parameter(PolicyId.KEY_SERVICE_METHOD, metadata.getMethod());
                        addCircuitBreaker(breakers, policy, api);
                        break;
                    default:
                        instancePolicies = addPolicy(policy, instancePolicies);
                }
            }
            // add listener before acquire permit
            invocation.addListener(new CircuitBreakerListener(this::getCircuitBreaker, errorParsers, breakers, instancePolicies));
            // acquire service permit
            acquire(breakers, Licensee::acquire, invocation);
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
        CircuitBreakInspector inspector;
        Double ratio;
        Double minRatio = null;
        CircuitBreakInfo status;
        for (CircuitBreakPolicy policy : policies) {
            inspector = policy.getInspector(endpoint.getId());
            status = inspector == null ? null : inspector.getInfo(now);
            if (status != null) {
                switch (status.getPhase()) {
                    case OPEN:
                        return false;
                    case RECOVER:
                        // in recover
                        ratio = status.getRecoverRatio();
                        if (ratio != null && (minRatio == null || ratio < minRatio)) {
                            minRatio = ratio;
                        }
                        break;
                    case CLOSED:
                        // Healthy nodes, delete.
                        policy.removeInspector(endpoint.getId(), inspector);
                        break;
                }
            }
        }
        if (minRatio != null) {
            endpoint.setWeightRatio(minRatio);
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
    private static void acquire(List<CircuitBreaker> circuitBreakers, Predicate<CircuitBreaker> predicate, OutboundInvocation<?> invocation) {
        acquire(circuitBreakers, predicate, breaker -> {
            DegradeConfig config = breaker.getPolicy().getDegradeConfig();
            if (config == null) {
                invocation.reject(FaultType.CIRCUIT_BREAK, "The traffic circuit break policy rejected the request.");
            } else {
                invocation.degrade(FaultType.CIRCUIT_BREAK, "The circuit break policy triggers a downgrade response.", config);
            }
        });
    }

    /**
     * Acquires permits from the list of circuit breakers using the provided predicate.
     * If acquiring a permit from any circuit breaker fails, it rolls back all previously acquired permits
     * and executes the provided fallback action.
     *
     * @param circuitBreakers The list of circuit breakers from which to acquire permits.
     * @param predicate       The predicate used to test whether a permit can be acquired from a circuit breaker.
     * @param fallback        The fallback action to execute if acquiring a permit fails. May be {@code null}.
     * @return {@code true} if permits were successfully acquired from all circuit breakers, {@code false} otherwise.
     */
    private static boolean acquire(List<CircuitBreaker> circuitBreakers,
                                   Predicate<CircuitBreaker> predicate,
                                   Consumer<CircuitBreaker> fallback) {
        if (!recover(circuitBreakers, fallback)) {
            // not acquire permits
            return false;
        }
        int acquires = 0;
        for (CircuitBreaker circuitBreaker : circuitBreakers) {
            if (!predicate.test(circuitBreaker)) {
                // failed to acquire permits
                rollback(circuitBreakers, acquires);
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
     * Rolls back the specified number of acquires for the given list of circuit breakers.
     *
     * @param circuitBreakers the list of circuit breakers to roll back
     * @param acquires        the number of acquires to roll back
     */
    private static void rollback(List<CircuitBreaker> circuitBreakers, int acquires) {
        if (acquires <= 0) {
            return;
        }
        // rollback
        int rollbacks = 0;
        for (CircuitBreaker breaker : circuitBreakers) {
            if (rollbacks++ < acquires) {
                breaker.release();
            } else {
                return;
            }
        }
    }

    /**
     * Attempts to recover a circuit breaker from a list of circuit breakers.
     * It selects the circuit breaker with the lowest recovery weight
     * and applies a fallback action if the random condition is met.
     *
     * @param circuitBreakers A list of circuit breakers to evaluate for recovery.
     * @param fallback        A consumer that defines the fallback action to be taken
     *                        if a circuit breaker is selected for recovery.
     * @return {@code true} if no circuit breaker is selected for recovery or
     * if the random condition is not met; {@code false} if a circuit
     * breaker is selected and the fallback action is applied.
     */
    private static boolean recover(List<CircuitBreaker> circuitBreakers, Consumer<CircuitBreaker> fallback) {
        CircuitBreaker minBreaker = null;
        Double ratio;
        Double minRatio = null;
        long now = System.currentTimeMillis();
        CircuitBreakInfo status;
        for (CircuitBreaker breaker : circuitBreakers) {
            status = breaker.getInfo(now);
            if (status != null && status.getPhase() == CircuitBreakPhase.RECOVER) {
                ratio = status.getRecoverRatio();
                if (ratio != null && (minRatio == null || ratio < minRatio)) {
                    minRatio = ratio;
                    minBreaker = breaker;
                }
            }
        }
        if (minBreaker != null) {
            int threshold = (int) (minRatio * 10000);
            if (ThreadLocalRandom.current().nextInt(10000) >= threshold) {
                if (fallback != null) {
                    fallback.accept(minBreaker);
                }
                return false;
            }
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
                        // append instance circuit breaker
                        circuitBreakers.add(breaker);
                    }
                }
                int size = circuitBreakers.size();
                if (size > index) {
                    int instances = invocation.getRouteTarget().size();
                    Predicate<CircuitBreaker> predicate = breaker -> breaker.acquireWhen(instances);
                    // acquire from instance circuit breaker
                    if (!acquire(circuitBreakers.subList(index, size), predicate, (Consumer<CircuitBreaker>) null)) {
                        // failed to acquire, rollback circuit breakers
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
