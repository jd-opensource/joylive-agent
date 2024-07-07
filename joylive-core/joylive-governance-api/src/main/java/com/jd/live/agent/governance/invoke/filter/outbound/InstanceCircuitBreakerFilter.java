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
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreaker;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerFactory;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilterChain;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.circuitbreaker.CircuitBreakerPolicy;
import com.jd.live.agent.governance.policy.service.circuitbreaker.CircuitLevel;
import com.jd.live.agent.governance.policy.service.circuitbreaker.DegradeConfig;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An instance-level circuit-breaker processes a circuit-break policy whose {@link CircuitLevel} is INSTANCE,
 * and uses the corresponding instance ID as the resource key to build a circuit-breaker. When a circuit break occurs,
 * the corresponding instance ID is temporarily marked as unavailable, and the instance is filtered
 * by {@link CircuitBreakerFilter} on the next request.
 *
 * @since 1.1.0
 */
@Injectable
@Extension(value = "InstanceCircuitBreakerFilter", order = OutboundFilter.ORDER_INSTANCE_CIRCUIT_BREAKER)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_CIRCUIT_BREAKER_ENABLED, matchIfMissing = true)
public class InstanceCircuitBreakerFilter implements OutboundFilter {

    @Inject
    @InjectLoader
    private Map<String, CircuitBreakerFactory> factories;

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, OutboundFilterChain chain) {
        RouteTarget target = invocation.getRouteTarget();
        List<? extends Endpoint> endpoints = target.getEndpoints();
        if (endpoints != null && endpoints.size() == 1) {
            Endpoint endpoint = endpoints.get(0);
            ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
            List<CircuitBreakerPolicy> policies = servicePolicy == null ? null : servicePolicy.getCircuitBreakerPolicies();
            if (null != policies && !policies.isEmpty()) {
                List<CircuitBreaker> circuitBreakers = new ArrayList<>(policies.size());
                for (CircuitBreakerPolicy policy : policies) {
                    if (policy.getLevel() != CircuitLevel.INSTANCE) {
                        CircuitBreakerFactory circuitBreakerFactory = factories.get(policy.getType());
                        CircuitBreaker circuitBreaker = circuitBreakerFactory.get(policy, policy.generateId(endpoint::getId).toString(),
                                name -> invocation.getContext().getPolicySupplier().getPolicy().getService(name));
                        circuitBreaker.setResourceKey(endpoint.getId());
                        circuitBreakers.add(circuitBreaker);
                    }
                }
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
                invocation.addListener(new CircuitBreakerFilter.CircuitBreakerListener(circuitBreakers));
            }
        }
        chain.filter(invocation);
    }
}
