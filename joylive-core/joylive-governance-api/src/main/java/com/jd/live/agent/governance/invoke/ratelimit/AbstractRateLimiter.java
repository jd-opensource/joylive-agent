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

import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import lombok.Getter;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * AbstractRateLimiter is an abstract implementation of the RateLimiter interface,
 * providing a foundation for concrete rate limiter implementations. It encapsulates
 * common functionality such as applying a rate limit policy and managing acquisition
 * timeouts.
 *
 * @since 1.0.0
 */
public abstract class AbstractRateLimiter implements RateLimiter {

    /**
     * The rate limit policy that defines the limits for the rate limiter.
     */
    @Getter
    protected final RateLimitPolicy policy;

    /**
     * The default timeout duration for permit acquisition.
     */
    protected final Duration timeout;

    /**
     * The option that contains additional settings that may affect the behavior of the rate limiter.
     */
    protected final Option option;

    @Getter
    private long lastAcquireTime;

    /**
     * Constructs a new AbstractRateLimiter with the specified rate limit policy.
     *
     * @param policy The rate limit policy to be applied by this rate limiter.
     */
    public AbstractRateLimiter(RateLimitPolicy policy) {
        this.policy = policy;
        this.option = MapOption.of(policy.getParameters());
        this.timeout = Duration.ofMillis(policy.getMaxWaitMs() == null || policy.getMaxWaitMs() < 0 ? 0 : policy.getMaxWaitMs());
    }

    @Override
    public boolean acquire() {
        this.lastAcquireTime = System.currentTimeMillis();
        return acquire(1, timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public boolean acquire(int permits) {
        this.lastAcquireTime = System.currentTimeMillis();
        return doAcquire(permits, timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public boolean acquire(int permits, long timeout, TimeUnit timeUnit) {
        this.lastAcquireTime = System.currentTimeMillis();
        return doAcquire(permits, timeout, timeUnit);
    }

    /**
     * Try to get some permits within a duration and return the result
     *
     * @param permits  Permits
     * @param timeout  Wait time
     * @param timeUnit Time unit
     * @return result
     */
    public abstract boolean doAcquire(int permits, long timeout, TimeUnit timeUnit);
}

