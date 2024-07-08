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

import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.InjectLoader;
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
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.circuitbreaker.CircuitBreakerPolicy;
import com.jd.live.agent.governance.policy.service.circuitbreaker.DegradeConfig;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CircuitBreakerFilter
 *
 * @since 1.1.0
 */
@Injectable
@Extension(value = "CircuitBreakerFilter", order = OutboundFilter.ORDER_CIRCUIT_BREAKER)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_CIRCUIT_BREAKER_ENABLED, matchIfMissing = true)
public class CircuitBreakerFilter implements OutboundFilter {

    @Inject
    @InjectLoader
    private Map<String, CircuitBreakerFactory> factories;

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, OutboundFilterChain chain) {
        ServiceMetadata metadata = invocation.getServiceMetadata();
        ServicePolicy servicePolicy = metadata.getServicePolicy();
        List<CircuitBreakerPolicy> policies = servicePolicy == null ? null : servicePolicy.getCircuitBreakerPolicies();
        if (null != policies && !policies.isEmpty()) {
            PolicySupplier policySupplier = invocation.getContext().getPolicySupplier();
            List<CircuitBreakerPolicy> instancePolicies = null;
            List<CircuitBreaker> serviceBreakers = new ArrayList<>(policies.size());
            CircuitBreaker breaker;
            for (CircuitBreakerPolicy policy : policies) {
                switch (policy.getLevel()) {
                    case SERVICE:
                        breaker = getCircuitBreaker(policy, policy.getUri(), policySupplier);
                        if (null != breaker) {
                            serviceBreakers.add(breaker);
                        }
                        break;
                    case API:
                        URI api = policy.getUri().path(metadata.getPath()).parameters(PolicyId.KEY_SERVICE_METHOD, metadata.getMethod());
                        breaker = getCircuitBreaker(policy, api, policySupplier);
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
                for (CircuitBreakerPolicy policy : instancePolicies) {
                    target.filter(endpoint -> !policy.isBroken(endpoint.getId(), currentTime));
                }
            }
        }
        chain.filter(invocation);
    }

    /**
     * Retrieves a circuit breaker for the given policy, URI, and policy supplier.
     *
     * @param policy         the circuit breaker policy.
     * @param uri            the URI for the circuit breaker.
     * @param policySupplier the policy supplier.
     * @return the circuit breaker, or null if no factory is found for the policy type.
     */
    private CircuitBreaker getCircuitBreaker(CircuitBreakerPolicy policy, URI uri, PolicySupplier policySupplier) {
        CircuitBreakerFactory factory = factories.get(policy.getType());
        return factory == null ? null : factory.get(policy, uri, policySupplier);
    }

    /**
     * Acquires permits from the list of circuit breakers for the given invocation.
     *
     * @param invocation      the outbound invocation.
     * @param circuitBreakers the list of circuit breakers.
     */
    private static void acquire(OutboundInvocation<?> invocation, List<CircuitBreaker> circuitBreakers) {
        for (CircuitBreaker circuitBreaker : circuitBreakers) {
            if (!circuitBreaker.acquire()) {
                DegradeConfig degradeConfig = circuitBreaker.getPolicy().getDegradeConfig();
                if (degradeConfig == null) {
                    invocation.reject(FaultType.CIRCUIT_BREAK, "The traffic circuit break policy rejects the request.");
                } else {
                    invocation.degrade(FaultType.CIRCUIT_BREAK, "The circuit break policy triggers a downgrade response.", degradeConfig);
                }
            }
        }
    }

    /**
     * A listener that handles circuit breaker events for outbound invocations.
     */
    private static class CircuitBreakerListener implements OutboundListener {

        private final CircuitBreakerFactory factory;

        private List<CircuitBreaker> circuitBreakers;

        private final List<CircuitBreakerPolicy> instancePolicies;

        private final int index;

        CircuitBreakerListener(CircuitBreakerFactory factory,
                               List<CircuitBreaker> circuitBreakers,
                               List<CircuitBreakerPolicy> instancePolicies) {
            this.factory = factory;
            this.circuitBreakers = circuitBreakers;
            this.instancePolicies = instancePolicies;
            this.index = circuitBreakers.size();
        }

        @Override
        public boolean onForward(Endpoint endpoint, OutboundInvocation<?> invocation) {
            if (endpoint != null && instancePolicies != null && !instancePolicies.isEmpty()) {
                PolicySupplier policySupplier = invocation.getContext().getPolicySupplier();
                for (CircuitBreakerPolicy policy : instancePolicies) {
                    URI uri = policy.getUri().parameter(PolicyId.KEY_SERVICE_ENDPOINT, endpoint.getId());
                    CircuitBreaker breaker = factory.get(policy, uri, policySupplier);
                    if (breaker != null) {
                        if (breaker.acquire()) {
                            circuitBreakers.add(breaker);
                        } else {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        @Override
        public void onCancel(Endpoint endpoint, OutboundInvocation<?> invocation) {
            int size = circuitBreakers.size();
            if (size > index) {
                List<CircuitBreaker> breakers = circuitBreakers.subList(index, size);
                breakers.forEach(CircuitBreaker::release);
                circuitBreakers = circuitBreakers.subList(0, index);
            }
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
            if (!(throwable instanceof CircuitBreakException)) {
                long duration = invocation.getRequest().getDuration();
                for (CircuitBreaker circuitBreaker : circuitBreakers) {
                    circuitBreaker.onError(duration, throwable);
                }
            }
        }
    }
}
