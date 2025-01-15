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
package com.jd.live.agent.implement.flowcontrol.ratelimit.redisson;

import com.jd.live.agent.governance.invoke.ratelimit.AbstractRateLimiterGroup;
import com.jd.live.agent.governance.invoke.ratelimit.RateLimiter;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;
import com.jd.live.agent.implement.flowcontrol.ratelimit.redisson.client.RedisClientManager;

/**
 * RedissonRateLimiterGroup
 *
 * @since 1.6.0
 */
public class RedissonRateLimiterGroup extends AbstractRateLimiterGroup {

    private final RedisClientManager manager;

    public RedissonRateLimiterGroup(RedisClientManager manager, RateLimitPolicy policy) {
        super(policy);
        this.manager = manager;
        init();
    }

    @Override
    protected RateLimiter create(SlidingWindow window, String name) {
        return new RedissonRateLimiter(manager, policy, window, name);
    }
}
