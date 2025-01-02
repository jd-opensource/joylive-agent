/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jd.live.agent.governance.invoke.ratelimit.leakybucket;

import com.jd.live.agent.governance.invoke.ratelimit.tokenbucket.TokenBucketLimiter;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LeakyBucketLimiter
 *
 * @since 1.4.0
 */
public class LeakyBucketLimiter extends TokenBucketLimiter {

    private static final String KEY_CAPACITY = "capacity";

    private final long capacity;

    private final AtomicLong requests = new AtomicLong(0);

    public LeakyBucketLimiter(RateLimitPolicy limitPolicy, SlidingWindow slidingWindow) {
        super(limitPolicy, slidingWindow);
        this.capacity = option.getLong(KEY_CAPACITY, 0L);
    }

    @Override
    protected double getMaxPermits() {
        return TimeUnit.SECONDS.toMicros(1L) / stableIntervalMicros;
    }

    @Override
    protected boolean isFull() {
        return capacity > 0 && requests.get() >= capacity;
    }

    @Override
    protected boolean doAcquire(int permits, long nowMicros, long timeoutMicros) {
        requests.incrementAndGet();
        try {
            return super.doAcquire(permits, nowMicros, timeoutMicros);
        } finally {
            requests.decrementAndGet();
        }

    }
}
