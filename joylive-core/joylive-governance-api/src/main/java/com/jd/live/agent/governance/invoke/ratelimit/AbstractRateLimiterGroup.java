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
    protected final List<RateLimiter> limiters = new ArrayList<>();

    /**
     * Constructs a new AbstractRateLimiterGroup with the specified rate limit policy.
     *
     * @param policy The rate limit policy to be applied to the limiters in the group.
     */
    public AbstractRateLimiterGroup(RateLimitPolicy policy) {
        super(policy, TimeUnit.NANOSECONDS);
        int i = 0;
        for (SlidingWindow window : policy.getSlidingWindows()) {
            limiters.add(create(window, policy.getName() + "-" + i++));
        }
    }

    /**
     * Creates a new rate limiter instance based on the provided sliding window and name.
     * This method must be implemented by subclasses to provide the specific rate limiter
     * instances that will be managed by this group.
     *
     * @param window The sliding window to be used by the rate limiter.
     * @param name   The name to be associated with the rate limiter.
     * @return A new rate limiter instance.
     */
    protected abstract RateLimiter create(SlidingWindow window, String name);

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

