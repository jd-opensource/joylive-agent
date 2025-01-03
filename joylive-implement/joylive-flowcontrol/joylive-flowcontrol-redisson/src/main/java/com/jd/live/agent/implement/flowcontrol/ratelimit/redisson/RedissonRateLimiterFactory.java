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

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.invoke.ratelimit.AbstractRateLimiterFactory;
import com.jd.live.agent.governance.invoke.ratelimit.RateLimiter;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;
import com.jd.live.agent.implement.flowcontrol.ratelimit.redisson.client.RedisClientManager;

import java.util.List;

/**
 * RedissonRateLimiterFactory
 *
 * @since 1.6.0
 */
@Injectable
@Extension(value = "RedissonCluster")
public class RedissonRateLimiterFactory extends AbstractRateLimiterFactory {

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    private transient volatile RedisClientManager manager;

    @Override
    protected RateLimiter create(RateLimitPolicy policy) {
        List<SlidingWindow> windows = policy.getSlidingWindows();
        RedisClientManager manager = getManager();
        return windows.size() == 1
                ? new RedissonRateLimiter(manager, policy, windows.get(0))
                : new RedissonRateLimiterGroup(manager, policy);
    }

    /**
     * Retrieves the singleton instance of {@link RedisClientManager}.
     *
     * @return The singleton instance of {@link RedisClientManager}.
     */
    private RedisClientManager getManager() {
        if (manager == null) {
            synchronized (this) {
                if (manager == null) {
                    manager = new RedisClientManager(timer);
                }
            }
        }
        return manager;
    }

}
