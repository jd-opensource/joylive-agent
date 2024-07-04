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

import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.InjectLoader;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.event.TrafficEvent;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreaker;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerFactory;
import com.jd.live.agent.governance.invoke.circuitbreak.ComposeCircuitBreaker;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilterChain;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.circuitbreaker.CircuitBreakerPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

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

    @Inject(Publisher.TRAFFIC)
    private Publisher<TrafficEvent> publisher;

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, OutboundFilterChain chain) {
        RouteTarget target = invocation.getRouteTarget();
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        List<CircuitBreakerPolicy> policies = servicePolicy == null ? null : servicePolicy.getCircuitBreakerPolicies();
        if (null != policies && !policies.isEmpty()) {
            CircuitBreaker circuitBreaker = invocation.getCircuitBreaker();
            if (circuitBreaker == null) {
                circuitBreaker = new ComposeCircuitBreaker(factories, policies, invocation);
                invocation.setCircuitBreaker(circuitBreaker);
            }
            if (!circuitBreaker.acquire()) {
                invocation.publish(publisher, TrafficEvent.builder().actionType(TrafficEvent.ActionType.REJECT).requests(1));
                invocation.reject(FaultType.LIMIT, "The traffic circuit-breaker policy rejects the request.");
            }
        }
        // TODO filter blocked instance
//        target.filter(Endpoint::isAccessible);
        chain.filter(invocation);
    }
}
