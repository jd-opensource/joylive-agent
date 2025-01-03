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

import java.util.concurrent.TimeUnit;

/**
 * RateLimiter
 *
 * @since 1.0.0
 */
public interface RateLimiter {

    /**
     * Try to get a permit return the result
     *
     * @return result
     */
    default boolean acquire() {
        return acquire(1);
    }

    /**
     * Retrieves the timestamp of the last successful acquisition.
     *
     * @return the timestamp of the last acquisition in milliseconds.
     */
    long getLastAcquireTime();

    /**
     * Try to get a permit within a duration and return the result
     *
     * @param timeout  Wait time
     * @param timeUnit Time unit
     * @return result
     */
    default boolean acquire(long timeout, TimeUnit timeUnit) {
        return acquire(1, timeout, timeUnit);
    }

    /**
     * Try to get some permits and return the result
     *
     * @param permits Permits
     * @return result
     */
    boolean acquire(int permits);

    /**
     * Try to get some permits within a duration and return the result
     *
     * @param permits  Permits
     * @param timeout  Wait time
     * @param timeUnit Time unit
     * @return result
     */
    boolean acquire(int permits, long timeout, TimeUnit timeUnit);

    /**
     * Get rate-limit policy
     *
     * @return policy
     */
    RateLimitPolicy getPolicy();

    default boolean isExpired(long expireTime) {
        return System.currentTimeMillis() - getLastAcquireTime() > expireTime;
    }

    default void recycle() {

    }
}
