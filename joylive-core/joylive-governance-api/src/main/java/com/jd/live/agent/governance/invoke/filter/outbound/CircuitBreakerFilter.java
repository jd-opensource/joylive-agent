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
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.circuitbreaker.CircuitBreakerPolicy;
import com.jd.live.agent.governance.policy.service.circuitbreaker.DegradeConfig;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

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
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        List<CircuitBreakerPolicy> policies = servicePolicy == null ? null : servicePolicy.getCircuitBreakerPolicies();
        if (null != policies && !policies.isEmpty()) {
            List<CircuitBreakerPolicy> servicePolicies = new ArrayList<>(policies.size());
            List<CircuitBreakerPolicy> instancePolicies = new ArrayList<>(policies.size());
            for (CircuitBreakerPolicy policy : policies) {
                switch (policy.getLevel()) {
                    case SERVICE:
                        servicePolicies.add(policy);
                        break;
                    case API:
                        // TODO handle api
                        break;
                    default:
                        instancePolicies.add(policy);
                }
            }

            List<CircuitBreaker> circuitBreakers = getCircuitBreakers(servicePolicies,
                    (policy, factory) -> factory.get(policy,
                            name -> invocation.getContext().getPolicySupplier().getPolicy().getService(name)));
            if (!instancePolicies.isEmpty()) {
                RouteTarget target = invocation.getRouteTarget();
                long currentTime = System.currentTimeMillis();
                for (CircuitBreakerPolicy policy : instancePolicies) {
                    target.filter(endpoint -> !policy.isBroken(endpoint.getId(), currentTime));
                }
            }
            // add listener before acquire permit
            invocation.addListener(new CircuitBreakerListener(invocation, circuitBreakers, instancePolicies));
            // acquire service permit
            acquire(invocation, circuitBreakers);
        }
        chain.filter(invocation);
    }

    private List<CircuitBreaker> getCircuitBreakers(List<CircuitBreakerPolicy> policies,
                                                    BiFunction<CircuitBreakerPolicy, CircuitBreakerFactory, CircuitBreaker> breakerFunction) {
        List<CircuitBreaker> circuitBreakers = new ArrayList<>(policies.size());
        for (CircuitBreakerPolicy policy : policies) {
            CircuitBreakerFactory circuitBreakerFactory = factories.get(policy.getType());
            CircuitBreaker circuitBreaker = breakerFunction.apply(policy, circuitBreakerFactory);
            if (circuitBreaker != null) {
                circuitBreakers.add(circuitBreaker);
            }
        }
        return circuitBreakers;
    }

    private void acquire(OutboundInvocation<?> invocation, List<CircuitBreaker> circuitBreakers) {
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

    public class CircuitBreakerListener implements OutboundListener {

        private final OutboundInvocation<?> invocation;

        private final List<CircuitBreaker> circuitBreakers;

        private final List<CircuitBreakerPolicy> instancePolicies;

        CircuitBreakerListener(OutboundInvocation<?> invocation,
                               List<CircuitBreaker> circuitBreakers,
                               List<CircuitBreakerPolicy> instancePolicies) {
            this.invocation = invocation;
            this.circuitBreakers = circuitBreakers;
            this.instancePolicies = instancePolicies;
        }

        @Override
        public void onForward(Endpoint endpoint, ServiceRequest request) {
            if (instancePolicies != null && !instancePolicies.isEmpty()) {
                List<CircuitBreaker> breakers = getCircuitBreakers(instancePolicies,
                        (policy, factory) -> factory.get(policy, policy.generateId(endpoint::getId).toString(),
                                name -> invocation.getContext().getPolicySupplier().getPolicy().getService(name)));
                circuitBreakers.addAll(breakers);
                acquire(invocation, breakers);
            }
        }

        @Override
        public void onSuccess(Endpoint endpoint, ServiceRequest request, ServiceResponse response) {
            for (CircuitBreaker circuitBreaker : circuitBreakers) {
                if (circuitBreaker.getPolicy().containsError(response.getCode())) {
                    circuitBreaker.onError(request.getDuration(), new CircuitBreakException("Exception of fuse response code"));
                } else {
                    circuitBreaker.onSuccess(request.getDuration());
                }
            }
        }

        @Override
        public void onFailure(Endpoint endpoint, ServiceRequest request, Throwable throwable) {
            if (!(throwable instanceof CircuitBreakException)) {
                for (CircuitBreaker circuitBreaker : circuitBreakers) {
                    circuitBreaker.onError(request.getDuration(), throwable);
                }
            }
        }
    }
}
