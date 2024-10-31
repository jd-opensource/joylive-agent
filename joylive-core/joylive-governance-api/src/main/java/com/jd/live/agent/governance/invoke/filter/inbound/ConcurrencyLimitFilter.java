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

import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
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
import java.util.concurrent.CompletionStage;

/**
 * ConcurrencyLimitFilter
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "ConcurrencyLimitFilter", order = InboundFilter.ORDER_CONCURRENCY_LIMITER)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class ConcurrencyLimitFilter implements InboundFilter, ExtensionInitializer {

    @Inject
    private Map<String, ConcurrencyLimiterFactory> factories;

    @Inject(nullable = true)
    private ConcurrencyLimiterFactory defaultFactory;

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    private GovernanceConfig governanceConfig;

    private String defaultType;

    @Override
    public void initialize() {
        defaultType = governanceConfig.getServiceConfig().getConcurrencyLimiter().getType();
    }

    @Override
    public <T extends InboundRequest> CompletionStage<Object> filter(InboundInvocation<T> invocation, InboundFilterChain chain) {
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        List<ConcurrencyLimitPolicy> concurrencyLimitPolicies = servicePolicy == null ? null : servicePolicy.getConcurrencyLimitPolicies();
        if (null != concurrencyLimitPolicies && !concurrencyLimitPolicies.isEmpty()) {
            for (ConcurrencyLimitPolicy policy : concurrencyLimitPolicies) {
                // match logic
                if (policy.getMaxConcurrency() != null && policy.getMaxConcurrency() > 0 && policy.match(invocation)) {
                    ConcurrencyLimiter limiter = getConcurrencyLimiter(policy);
                    if (null != limiter && !limiter.acquire()) {
                        invocation.reject(FaultType.LIMIT,
                                "The request is rejected by concurrency limiter. maxConcurrency=" +
                                        policy.getMaxConcurrency());
                    }
                }
            }
        }
        return chain.filter(invocation);
    }

    /**
     * Retrieves a concurrency limiter based on the given policy.
     * If the policy's realize type is not specified, it falls back to the default type
     * from the service configuration. If the factory for the specified type is not found,
     * it uses the first available factory.
     *
     * @param policy the concurrency limit policy.
     * @return the concurrency limiter, or null if no factory is found for the policy type.
     */
    private ConcurrencyLimiter getConcurrencyLimiter(ConcurrencyLimitPolicy policy) {
        String type = policy.getRealizeType() == null || policy.getRealizeType().isEmpty() ? defaultType : policy.getRealizeType();
        ConcurrencyLimiterFactory factory = type != null ? factories.get(type) : null;
        factory = factory == null ? defaultFactory : factory;
        return factory == null ? null : factory.get(policy);
    }
}
