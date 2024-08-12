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
package com.jd.live.agent.governance.invoke.filter.outbound;

import com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException;
import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.exception.CircuitBreakException;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundListener;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreaker;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerFactory;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilterChain;
import com.jd.live.agent.governance.invoke.metadata.ServiceMetadata;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * CircuitBreakerFilter
 *
 * @since 1.1.0
 */
@Injectable
@Extension(value = "CircuitBreakerFilter", order = OutboundFilter.ORDER_CIRCUIT_BREAKER)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_CIRCUIT_BREAKER_ENABLED, matchIfMissing = true)
public class CircuitBreakerFilter implements OutboundFilter, ExtensionInitializer {

    @Inject
    private Map<String, CircuitBreakerFactory> factories;

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
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, OutboundFilterChain chain) {
        ServiceMetadata metadata = invocation.getServiceMetadata();
        ServicePolicy servicePolicy = metadata.getServicePolicy();
        List<CircuitBreakPolicy> policies = servicePolicy == null ? null : servicePolicy.getCircuitBreakPolicies();
        if (null != policies && !policies.isEmpty()) {
            List<CircuitBreakPolicy> instancePolicies = null;
            List<CircuitBreaker> serviceBreakers = new ArrayList<>(policies.size());
            CircuitBreaker breaker;
            for (CircuitBreakPolicy policy : policies) {
                switch (policy.getLevel()) {
                    case SERVICE:
                        breaker = getCircuitBreaker(policy, policy.getUri());
                        if (null != breaker) {
                            serviceBreakers.add(breaker);
                        }
                        break;
                    case API:
                        URI api = policy.getUri().path(metadata.getPath()).parameters(PolicyId.KEY_SERVICE_METHOD, metadata.getMethod());
                        breaker = getCircuitBreaker(policy, api);
                        if (null != breaker) {
                            serviceBreakers.add(breaker);
                        }
                        break;
                    default:
                        if (instancePolicies == null) {
                            instancePolicies = new ArrayList<>(policies.size());
                        }
                        instancePolicies.add(policy);
                }
            }
            // add listener before acquire permit
            invocation.addListener(new CircuitBreakerListener(this::getCircuitBreaker, serviceBreakers, instancePolicies));
            // acquire service permit
            acquire(invocation, serviceBreakers);
            // filter broken instance
            if (instancePolicies != null && !instancePolicies.isEmpty()) {
                RouteTarget target = invocation.getRouteTarget();
                long currentTime = System.currentTimeMillis();
                for (CircuitBreakPolicy policy : instancePolicies) {
                    target.filter(endpoint -> !policy.isBroken(endpoint.getId(), currentTime));
                }
            }
        }
        chain.filter(invocation);
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
                invocation.reject(FaultType.CIRCUIT_BREAK, "The traffic circuit break policy rejects the request.");
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

        private List<CircuitBreaker> circuitBreakers;

        private final List<CircuitBreakPolicy> instancePolicies;

        private final int index;

        CircuitBreakerListener(CircuitBreakerFactory factory,
                               List<CircuitBreaker> circuitBreakers,
                               List<CircuitBreakPolicy> instancePolicies) {
            this.factory = factory;
            this.circuitBreakers = circuitBreakers;
            this.instancePolicies = instancePolicies;
            this.index = circuitBreakers.size();
        }

        @Override
        public boolean onElect(Endpoint endpoint, OutboundInvocation<?> invocation) {
            if (endpoint != null && instancePolicies != null && !instancePolicies.isEmpty()) {
                for (CircuitBreakPolicy policy : instancePolicies) {
                    URI uri = policy.getUri().parameter(PolicyId.KEY_SERVICE_ENDPOINT, endpoint.getId());
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
            long duration = invocation.getRequest().getDuration();
            for (CircuitBreaker circuitBreaker : circuitBreakers) {
                if (response != null && circuitBreaker.getPolicy().containsError(response.getCode())) {
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
                String code = request.getErrorCode(throwable);
                Throwable cause = request.getCause(throwable);
                for (CircuitBreaker circuitBreaker : circuitBreakers) {
                    CircuitBreakPolicy policy = circuitBreaker.getPolicy();
                    if (policy.containsError(code) || policy.containsException(cause)) {
                        circuitBreaker.onError(duration, cause);
                    } else {
                        circuitBreaker.onSuccess(duration);
                    }
                }
            }
        }
    }

}
