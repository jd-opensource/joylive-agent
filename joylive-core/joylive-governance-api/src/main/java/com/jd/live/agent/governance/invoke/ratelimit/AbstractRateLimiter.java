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
import com.jd.live.agent.governance.invoke.permission.AbstractLicensee;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import lombok.Getter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AbstractRateLimiter is an abstract implementation of the RateLimiter interface,
 * providing a foundation for concrete rate limiter implementations. It encapsulates
 * common functionality such as applying a rate limit policy and managing acquisition
 * timeouts.
 *
 * @since 1.0.0
 */
public abstract class AbstractRateLimiter extends AbstractLicensee<RateLimitPolicy> implements RateLimiter {

    protected static final Long MICROSECOND_OF_ONE_SECOND = 1000 * 1000L;

    /**
     * The default time unit.
     */
    protected final TimeUnit timeUnit;

    /**
     * The default timeout duration for permit acquisition.
     */
    protected final long timeout;

    /**
     * The option that contains additional settings that may affect the behavior of the rate limiter.
     */
    protected final Option option;

    @Getter
    protected long lastAccessTime;

    protected final AtomicBoolean started = new AtomicBoolean(true);

    /**
     * Constructs an instance of the AbstractRateLimiter class with the given rate limit policy and time unit.
     * @param policy the rate limit policy to use
     * @param timeUnit the time unit to use for rate limiting
     */
    public AbstractRateLimiter(RateLimitPolicy policy, TimeUnit timeUnit) {
        this.policy = policy;
        this.timeUnit = timeUnit;
        this.timeout = timeUnit.convert(policy.getMaxWaitMs() == null || policy.getMaxWaitMs() < 0 ? 0 : policy.getMaxWaitMs(), TimeUnit.MILLISECONDS);
        this.option = MapOption.of(policy.getParameters());
    }

    @Override
    public boolean acquire() {
        if (!started.get()) {
            return true;
        }
        this.lastAccessTime = System.currentTimeMillis();
        return doAcquire(1, timeout, timeUnit);
    }

    @Override
    public boolean acquire(int permits) {
        if (!started.get()) {
            return true;
        }
        this.lastAccessTime = System.currentTimeMillis();
        return permits <= 0 || doAcquire(permits, timeout, timeUnit);
    }

    @Override
    public boolean acquire(int permits, long timeout, TimeUnit timeUnit) {
        if (!started.get()) {
            return true;
        }
        this.lastAccessTime = System.currentTimeMillis();
        return permits <= 0 || doAcquire(permits, timeout, timeUnit);
    }

    @Override
    public void close() {
        if (started.compareAndSet(true, false)) {
            doClose();
        }
    }

    /**
     * Try to get some permits within a duration and return the result
     *
     * @param permits  Permits
     * @param timeout  Wait time
     * @param timeUnit Time unit
     * @return result
     */
    protected abstract boolean doAcquire(int permits, long timeout, TimeUnit timeUnit);

    /**
     * Closes the limiter.
     */
    protected void doClose() {

    }

}

