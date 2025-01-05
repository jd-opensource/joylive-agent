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
package com.jd.live.agent.governance.invoke.concurrencylimit;

import com.jd.live.agent.governance.config.RecyclerConfig;
import com.jd.live.agent.governance.invoke.permission.AbstractLicenseeFactory;
import com.jd.live.agent.governance.policy.service.limit.ConcurrencyLimitPolicy;

/**
 * AbstractConcurrencyLimiterFactory
 */
public abstract class AbstractConcurrencyLimiterFactory
        extends AbstractLicenseeFactory<ConcurrencyLimitPolicy, Long, ConcurrencyLimiter>
        implements ConcurrencyLimiterFactory {

    @Override
    public ConcurrencyLimiter get(ConcurrencyLimitPolicy policy) {
        return get(policy, policy == null ? null : policy.getId(), null, () -> create(policy));
    }

    @Override
    protected RecyclerConfig getConfig() {
        return governanceConfig.getServiceConfig().getConcurrencyLimiter();
    }

    /**
     * Retrieves a new instance of a {@link ConcurrencyLimiter} based on the provided
     * concurrency limit policy.
     *
     * @param policy the policy that defines the concurrency limiting rules.
     * @return a new instance of a concurrency limiter configured according to the policy.
     */
    protected abstract ConcurrencyLimiter create(ConcurrencyLimitPolicy policy);
}
