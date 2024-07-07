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
import com.jd.live.agent.governance.policy.service.circuitbreaker.CircuitLevel;
import com.jd.live.agent.governance.policy.service.circuitbreaker.DegradeConfig;
import com.jd.live.agent.governance.request.ServiceRequest;
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

    @Inject(Publisher.TRAFFIC)
    private Publisher<TrafficEvent> publisher;

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, OutboundFilterChain chain) {
        // TODO only service circuit, how api circuit work?
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        List<CircuitBreakerPolicy> policies = servicePolicy == null ? null : servicePolicy.getCircuitBreakerPolicies();
        if (null != policies && !policies.isEmpty()) {
            List<CircuitBreaker> circuitBreakers = getCircuitBreakers(invocation, policies);
            // add listener before acquire permit
            invocation.addListener(new CircuitBreakerListener(circuitBreakers));
            acquire(invocation, circuitBreakers);
        }
        chain.filter(invocation);
    }

    private <T extends OutboundRequest> List<CircuitBreaker> getCircuitBreakers(OutboundInvocation<T> invocation,
                                                                                List<CircuitBreakerPolicy> policies) {
        RouteTarget target = invocation.getRouteTarget();
        List<CircuitBreaker> circuitBreakers = new ArrayList<>(policies.size());
        for (CircuitBreakerPolicy policy : policies) {
            if (policy.getLevel() == CircuitLevel.SERVICE) {
                CircuitBreakerFactory circuitBreakerFactory = factories.get(policy.getType());
                CircuitBreaker circuitBreaker = circuitBreakerFactory.get(policy,
                        name -> invocation.getContext().getPolicySupplier().getPolicy().getService(name));
                circuitBreakers.add(circuitBreaker);
            }
            if (policy.getLevel() == CircuitLevel.INSTANCE) {
                long currentTime = System.currentTimeMillis();
                target.filter(endpoint -> {
                    Long endTime = policy.getBlockedEndpoints().get(endpoint.getId());
                    if (endTime == null) {
                        return true;
                    }
                    if (endTime <= currentTime) {
                        policy.getBlockedEndpoints().remove(endpoint.getId());
                        return true;
                    }
                    return false;
                });
            }
        }
        return circuitBreakers;
    }

    private <T extends OutboundRequest> void acquire(OutboundInvocation<T> invocation, List<CircuitBreaker> circuitBreakers) {
        for (CircuitBreaker circuitBreaker : circuitBreakers) {
            if (!circuitBreaker.acquire()) {
                DegradeConfig degradeConfig = circuitBreaker.getPolicy().getDegradeConfig();
                if (degradeConfig == null) {
                    // TODO more circuit breaker metrics data
                    invocation.publish(publisher, TrafficEvent.builder().actionType(TrafficEvent.ActionType.REJECT).requests(1));
                    invocation.reject(FaultType.CIRCUIT_BREAK, "The traffic circuit-breaker policy rejects the request.");
                } else {
                    invocation.degrade(FaultType.CIRCUIT_BREAK, "The fuse strategy triggers a downgrade response.", degradeConfig);
                }
            }
        }
    }

    public static class CircuitBreakerListener implements OutboundListener {

        private final List<CircuitBreaker> circuitBreakers;

        CircuitBreakerListener(List<CircuitBreaker> circuitBreakers) {
            this.circuitBreakers = circuitBreakers;
        }

        @Override
        public void onSuccess(Endpoint endpoint, ServiceRequest request, ServiceResponse response) {
            for (CircuitBreaker circuitBreaker : circuitBreakers) {
                CircuitBreakerPolicy policy = circuitBreaker.getPolicy();
                if (policy.containsError(response.getCode())) {
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
