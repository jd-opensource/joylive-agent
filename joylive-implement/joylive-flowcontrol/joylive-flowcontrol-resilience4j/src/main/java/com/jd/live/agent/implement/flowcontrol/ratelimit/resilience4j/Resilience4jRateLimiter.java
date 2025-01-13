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
package com.jd.live.agent.implement.flowcontrol.ratelimit.resilience4j;

import com.jd.live.agent.governance.invoke.ratelimit.AbstractRateLimiter;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Resilience4jRateLimiter
 *
 * @since 1.0.0
 */
public class Resilience4jRateLimiter extends AbstractRateLimiter {

    private final AtomicRateLimiter limiter;

    public Resilience4jRateLimiter(RateLimitPolicy policy, SlidingWindow window) {
        this(policy, window, policy.getName());
    }

    public Resilience4jRateLimiter(RateLimitPolicy policy, SlidingWindow window, String name) {
        super(policy, TimeUnit.NANOSECONDS);
        // Create a RateLimiter
        this.limiter = new AtomicRateLimiter(name, RateLimiterConfig.custom()
                .timeoutDuration(Duration.ofNanos(timeout)) // the timeout is nanoseconds
                .limitRefreshPeriod(Duration.ofMillis(window.getTimeWindowInMs()))
                .limitForPeriod(window.getThreshold())
                .build());
    }

    @Override
    protected boolean doAcquire(int permits, long timeout, TimeUnit timeUnit) {
        return limiter.acquirePermission(permits, timeout, timeUnit);
    }
}
