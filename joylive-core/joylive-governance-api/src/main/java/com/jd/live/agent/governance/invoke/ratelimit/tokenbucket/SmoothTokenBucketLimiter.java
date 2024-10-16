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
package com.jd.live.agent.governance.invoke.ratelimit.tokenbucket;

import com.jd.live.agent.governance.invoke.ratelimit.AbstractRateLimiter;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;

import java.util.concurrent.TimeUnit;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * SmoothTokenBucketLimiter
 * <p>
 * Source code implementation borrows from Guava's com.google.common.util.concurrent.RateLimiter
 * </p>
 *
 * @since 1.0.0
 */
public class SmoothTokenBucketLimiter extends AbstractRateLimiter {

    private final static String KEY_MAX_BURST_SECONDS = "maxBurstSeconds";

    protected final SleepingStopwatch stopwatch;

    /**
     * The maximum number of stored permits.
     */
    protected final double maxPermits;

    /**
     * The interval between two unit requests, at our stable rate. E.g., a stable rate of 5 permits
     * per second has a stable interval of 200ms.
     */
    protected final double stableIntervalMicros;

    protected long nextFreeTicketMicros;

    protected volatile Object mutexDoNotUseDirectly;

    /**
     * The currently stored permits.
     */
    private double storedPermits;

    public SmoothTokenBucketLimiter(RateLimitPolicy limitPolicy, SlidingWindow slidingWindow) {
        super(limitPolicy);
        int maxBurstSeconds = option.getInteger(KEY_MAX_BURST_SECONDS, 5);
        double secondPermits = slidingWindow.getSecondPermits();
        this.stopwatch = SleepingStopwatch.createFromSystemTimer();
        this.stableIntervalMicros = secondPermits <= 0 ? 100 : TimeUnit.SECONDS.toMicros(1L) / secondPermits;
        this.maxPermits = maxBurstSeconds * secondPermits;
        this.nextFreeTicketMicros = stopwatch.readMicros();
        this.storedPermits = 0.0;
    }

    @Override
    public boolean acquire() {
        return acquire(1, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean acquire(int permits) {
        return acquire(permits, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean doAcquire(int permits, long timeout, TimeUnit timeUnit) {
        if (permits <= 0) {
            return false;
        }
        long timeoutMicros = timeUnit.toMicros(timeout < 0 ? 0 : timeout);
        long nowMicros = stopwatch.readMicros();
        synchronized (mutex()) {
            if (!isAvailable(nowMicros, timeoutMicros)) {
                return false;
            }
            doAcquire(permits, nowMicros);
        }
        return true;
    }

    /**
     * Acquires the specified number of permits, blocking until they become available.
     *
     * @param permits   the number of permits to acquire
     * @param nowMicros the current time in microseconds
     */
    protected void doAcquire(int permits, long nowMicros) {
        // always pay in advance
        stopwatch.sleepMicrosUninterruptibly(waitFor(permits, nowMicros));
    }

    /**
     * Checks if the current time is before the timeout time for acquiring a free ticket.
     *
     * @param nowMicros     the current time in microseconds
     * @param timeoutMicros the timeout time in microseconds
     * @return true if the current time is before the timeout time, false otherwise
     */
    protected boolean isAvailable(long nowMicros, long timeoutMicros) {
        return nextFreeTicketMicros - timeoutMicros <= nowMicros;
    }

    /**
     * Waits for the specified number of permits to become available.
     *
     * @param permits   the number of permits to wait for
     * @param nowMicros the current time in microseconds
     * @return the time waited in microseconds, or 0 if no wait was necessary
     */
    protected long waitFor(long permits, long nowMicros) {
        long momentAvailable = waitForEarliestAvailable(permits, nowMicros);
        return max(momentAvailable - nowMicros, 0);
    }

    /**
     * Waits for the specified number of permits to become available, returning the time at which the earliest permit will be available.
     *
     * @param permits   the number of permits to wait for
     * @param nowMicros the current time in microseconds
     * @return the time at which the earliest permit will be available, in microseconds
     */
    protected long waitForEarliestAvailable(long permits, long nowMicros) {
        update(nowMicros);
        long returnValue = nextFreeTicketMicros;
        double available = min(permits, storedPermits);

        double lack = permits - available;
        long waitMicros = (long) (lack * stableIntervalMicros);

        nextFreeTicketMicros = saturatedAdd(nextFreeTicketMicros, waitMicros);
        storedPermits -= available;
        return returnValue;
    }

    /**
     * Updates the internal state of the rate limiter based on the current time.
     *
     * @param nowMicros the current time in microseconds
     */
    protected void update(long nowMicros) {
        // if nextFreeTicket is in the past, reSync to now
        if (nowMicros > nextFreeTicketMicros) {
            double newPermits = (nowMicros - nextFreeTicketMicros) / stableIntervalMicros;
            storedPermits = min(maxPermits, storedPermits + newPermits);
            nextFreeTicketMicros = nowMicros;
        }
    }

    /**
     * Returns the mutex object used for synchronization.
     *
     * @return the mutex object
     */
    private Object mutex() {
        Object mutex = mutexDoNotUseDirectly;
        if (mutex == null) {
            synchronized (this) {
                mutex = mutexDoNotUseDirectly;
                if (mutex == null) {
                    mutexDoNotUseDirectly = mutex = new Object();
                }
            }
        }
        return mutex;
    }

    /**
     * Adds two long values, handling overflow by returning the maximum or minimum value.
     *
     * @param a the first value to add
     * @param b the second value to add
     * @return the sum of the two values, or Long.MAX_VALUE if the result overflows, or Long.MIN_VALUE if the result underflows
     */
    protected long saturatedAdd(long a, long b) {
        long naiveSum = a + b;
        if ((a ^ b) < 0 | (a ^ naiveSum) >= 0) {
            // If a and b have different signs or a has the same sign as the result then there was no
            // overflow, return.
            return naiveSum;
        }
        // we did over/under flow, if the sign is negative we should return MAX otherwise MIN
        return Long.MAX_VALUE + ((naiveSum >>> (Long.SIZE - 1)) ^ 1);
    }
}
