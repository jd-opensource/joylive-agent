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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.invoke.ratelimit.AbstractRateLimiter;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;
import com.jd.live.agent.implement.flowcontrol.ratelimit.redisson.client.RedisClient;
import com.jd.live.agent.implement.flowcontrol.ratelimit.redisson.client.RedisClientManager;
import com.jd.live.agent.implement.flowcontrol.ratelimit.redisson.client.RedisConfig;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * RedissonRateLimiter
 *
 * @since 1.6.0
 */
public class RedissonRateLimiter extends AbstractRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(RedissonRateLimiter.class);

    private final RedisClient client;

    private final RRateLimiter limiter;

    public RedissonRateLimiter(RedisClientManager manager, RateLimitPolicy policy, SlidingWindow window) {
        this(manager, policy, window, policy.getName());
    }

    public RedissonRateLimiter(RedisClientManager manager, RateLimitPolicy policy, SlidingWindow window, String name) {
        super(policy, TimeUnit.MILLISECONDS);
        this.client = manager.getOrCreateClient(new RedisConfig(policy.getId(), option));
        this.limiter = client.getRateLimiter("LiveAgent-limiter-" + policy.getId());
        if (limiter != null) {
            limiter.trySetRate(RateType.OVERALL, window.getThreshold(), Duration.ofMillis(window.getTimeWindowInMs()));
        }
    }

    @Override
    protected boolean doAcquire(int permits, long timeout, TimeUnit timeUnit) {
        client.setLastAccessTime(System.currentTimeMillis());
        try {
            return limiter == null || limiter.tryAcquire(permits, Duration.ofNanos(timeUnit.toNanos(timeout)));
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            return true;
        }
    }

    @Override
    protected void doClose() {
        client.close();
    }
}
