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
package com.jd.live.agent.governance.invoke.filter.inbound;

import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.InjectLoader;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.concurrencylimit.ConcurrencyLimiter;
import com.jd.live.agent.governance.invoke.concurrencylimit.ConcurrencyLimiterFactory;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.InboundFilterChain;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.limit.ConcurrencyLimitPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;

import java.util.List;
import java.util.Map;

/**
 * ConcurrencyLimitInboundFilter
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "ConcurrencyLimitInboundFilter", order = InboundFilter.ORDER_INBOUND_LIMITER)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class ConcurrencyLimitInboundFilter implements InboundFilter {

    @Inject
    @InjectLoader
    private Map<String, ConcurrencyLimiterFactory> factories;

    @Override
    public <T extends InboundRequest> void filter(InboundInvocation<T> invocation, InboundFilterChain chain) {
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        List<ConcurrencyLimitPolicy> concurrencyLimitPolicies = servicePolicy == null ? null : servicePolicy.getConcurrencyLimitPolicies();
        if (null != concurrencyLimitPolicies && !concurrencyLimitPolicies.isEmpty()) {
            for (ConcurrencyLimitPolicy policy : concurrencyLimitPolicies) {
                // match logic
                if (policy.match(invocation)) {
                    ConcurrencyLimiterFactory concurrencyLimiterFactory = factories.get(policy.getStrategyType());
                    ConcurrencyLimiter limiter = concurrencyLimiterFactory.get(policy);
                    if (null != limiter && !limiter.acquire()) {
                        invocation.reject(FaultType.LIMIT, "The traffic limiting policy rejects the request.");
                    }
                }
            }
        }
        chain.filter(invocation);
    }
}
