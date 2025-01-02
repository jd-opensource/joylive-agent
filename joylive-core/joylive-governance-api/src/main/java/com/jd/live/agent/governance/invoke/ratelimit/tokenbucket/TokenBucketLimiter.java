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
 * TokenBucketLimiter
 * <p>
 * Source code implementation borrows from Guava's com.google.common.util.concurrent.SmoothRateLimiter.SmoothBursty
 * </p>
 *
 * @since 1.0.0
 */
public abstract class TokenBucketLimiter extends AbstractRateLimiter {

    private static final int DEFAULT_SECOND_PERMITS = 1000;

    protected final SleepingStopwatch stopwatch;

    /**
     * The maximum number of stored permits.
     */
    protected double maxPermits;
    /**
     * The interval between two unit requests, at our stable rate. E.g., a stable rate of 5 permits
     * per second has a stable interval of 200ms.
     */
    protected double stableIntervalMicros;

    protected long nextFreeTicketMicros;

    protected final Object mutex = new Object();

    /**
     * The currently stored permits.
     */
    private double storedPermits;

    public TokenBucketLimiter(RateLimitPolicy limitPolicy, SlidingWindow slidingWindow) {
        super(limitPolicy, TimeUnit.MILLISECONDS);
        this.stopwatch = SleepingStopwatch.createFromSystemTimer();
        double secondPermits = slidingWindow.getSecondPermits();
        this.stableIntervalMicros = secondPermits <= 0 ? DEFAULT_SECOND_PERMITS : TimeUnit.SECONDS.toMicros(1L) / secondPermits;
        initialize();
        update(stopwatch.readMicros());
    }

    @Override
    protected boolean doAcquire(int permits, long timeout, TimeUnit timeUnit) {
        long timeoutMicros = timeout <= 0 ? 0 : timeUnit.toMicros(timeout);
        long nowMicros = stopwatch.readMicros();
        if (isTimeout(nowMicros, timeoutMicros) && isFull()) {
            return false;
        }
        return doAcquire(permits, nowMicros, timeoutMicros);
    }

    /**
     * Attempts to acquire the specified number of permits,
     * waiting if necessary until the permits become available or the specified timeout expires.
     *
     * @param permits       the number of permits to acquire
     * @param nowMicros     the current time in microseconds
     * @param timeoutMicros the maximum time to wait in microseconds
     * @return true if the permits were acquired, false if the timeout expired
     */
    protected boolean doAcquire(int permits, long nowMicros, long timeoutMicros) {
        long microsToWait;
        synchronized (mutex) {
            // double check in lock
            if (isTimeout(nowMicros, timeoutMicros)) {
                return false;
            }
            microsToWait = computeWaitFor(permits, nowMicros);
        }
        stopwatch.sleepMicrosUninterruptibly(microsToWait);
        return true;
    }

    /**
     * Initializes the rate limiter by setting the maximum number of permits.
     */
    protected void initialize() {
        this.maxPermits = getMaxPermits();
    }

    /**
     * Calculates and returns the maximum number of permits that can be accumulated.
     *
     * @return the maximum number of permits
     */
    protected abstract double getMaxPermits();

    /**
     * Checks if the current time is before the timeout time for acquiring a free ticket.
     *
     * @param nowMicros     the current time in microseconds
     * @param timeoutMicros the timeout time in microseconds
     * @return true if the current time is before the timeout time, false otherwise
     */
    protected boolean isTimeout(long nowMicros, long timeoutMicros) {
        return nextFreeTicketMicros > nowMicros + timeoutMicros;
    }

    /**
     * Checks if the current state is full.
     *
     * @return {@code true} if the state is full, {@code false} otherwise.
     */
    protected boolean isFull() {
        return false;
    }

    /**
     * Waits for the specified number of permits to become available.
     *
     * @param permits   the number of permits to wait for
     * @param nowMicros the current time in microseconds
     * @return the time waited in microseconds, or 0 if no wait was necessary
     */
    protected long computeWaitFor(long permits, long nowMicros) {
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
        long waitMicros = waitForStoredPermits(storedPermits, available) + (long) (lack * stableIntervalMicros);

        nextFreeTicketMicros = saturatedAdd(nextFreeTicketMicros, waitMicros);
        storedPermits -= available;
        return returnValue;
    }

    /**
     * Waits for the specified number of stored permits to become available.
     *
     * @param storedPermits the current number of stored permits
     * @param permitsToTake the number of permits to wait for
     * @return the time waited in microseconds
     */
    protected long waitForStoredPermits(double storedPermits, double permitsToTake) {
        return 0L;
    }

    /**
     * Returns the number of microseconds during cool down that we have to wait to get a new permit.
     */
    protected double coolDownIntervalMicros() {
        return stableIntervalMicros;
    }

    /**
     * Updates the internal state of the rate limiter based on the current time.
     *
     * @param nowMicros the current time in microseconds
     */
    protected void update(long nowMicros) {
        if (nowMicros > nextFreeTicketMicros) {
            // if nextFreeTicket is in the past.
            double newPermits = (nowMicros - nextFreeTicketMicros) / coolDownIntervalMicros();
            storedPermits = min(maxPermits, storedPermits + newPermits);
            nextFreeTicketMicros = nowMicros;
        }
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
