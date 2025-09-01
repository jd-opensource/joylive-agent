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

import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * An abstract implementation of a rate limiter group, which is a collection of
 * individual rate limiters that are managed together to enforce a combined
 * rate limiting policy.
 *
 * @since 1.0.0
 */
public abstract class AbstractRateLimiterGroup extends AbstractRateLimiter {

    /**
     * A list of rate limiters that are part of this group.
     */
    protected final List<RateLimiter> limiters;

    /**
     * Constructs a new AbstractRateLimiterGroup with the specified rate limit policy.
     *
     * @param policy The rate limit policy to be applied to the limiters in the group.
     */
    public AbstractRateLimiterGroup(RateLimitPolicy policy, BiFunction<SlidingWindow, String, RateLimiter> function) {
        super(policy, TimeUnit.NANOSECONDS);
        this.limiters = createLimiters(policy, function);
    }

    /**
     * Creates rate limiters for each sliding window in the policy.
     *
     * @param policy the rate limit policy containing sliding windows
     * @param function the factory function to create individual rate limiters
     * @return list of created rate limiters
     */
    protected List<RateLimiter> createLimiters(RateLimitPolicy policy, BiFunction<SlidingWindow, String, RateLimiter> function) {
        List<RateLimiter> limiters = new ArrayList<>();
        int i = 0;
        for (SlidingWindow window : policy.getSlidingWindows()) {
            limiters.add(function.apply(window, policy.getLimiterName(String.valueOf(i))));
            i++;
        }
        return limiters;
    }

    @Override
    protected boolean doAcquire(int permits, long timeout, TimeUnit timeUnit) {
        // Convert to nanoseconds to avoid losing precision.
        long startTime = System.nanoTime();
        timeout = timeout <= 0 ? 0 : timeUnit.toNanos(timeout);
        long expire;
        for (RateLimiter limiter : limiters) {
            expire = timeout - (System.nanoTime() - startTime);
            expire = Long.max(0, expire);
            if (!limiter.acquire(permits, expire, TimeUnit.NANOSECONDS)) {
                return false;
            }
        }
        return true;
    }
}

