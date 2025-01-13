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
package com.jd.live.agent.governance.invoke.ratelimit;

import com.jd.live.agent.governance.config.RecyclerConfig;
import com.jd.live.agent.governance.invoke.permission.AbstractLicenseeFactory;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;

/**
 * AbstractLimiterFactory provides a base implementation for factories that create and manage rate limiters.
 * It uses a thread-safe map to store and retrieve rate limiters associated with specific rate limit policies.
 * This class is designed to be extended by concrete factory implementations that provide the actual
 * rate limiter creation logic.
 *
 * @since 1.0.0
 */
public abstract class AbstractRateLimiterFactory
        extends AbstractLicenseeFactory<RateLimitPolicy, Long, RateLimiter>
        implements RateLimiterFactory {

    @Override
    public RateLimiter get(RateLimitPolicy policy) {
        return get(policy, policy == null ? null : policy.getId(), p -> p.getSlidingWindowSize() > 0, () -> create(policy));
    }

    @Override
    protected RecyclerConfig getConfig() {
        return governanceConfig.getServiceConfig().getRateLimiter();
    }

    /**
     * Creates a new rate limiter instance based on the provided rate limit policy.
     * This method is abstract and must be implemented by subclasses to provide the specific
     * rate limiter creation logic.
     *
     * @param policy The rate limit policy to be used for creating the rate limiter.
     * @return A new rate limiter instance that enforces the given policy.
     */
    protected abstract RateLimiter create(RateLimitPolicy policy);

}

