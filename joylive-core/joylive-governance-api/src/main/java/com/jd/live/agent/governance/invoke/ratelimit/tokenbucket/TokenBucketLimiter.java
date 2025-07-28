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

    protected static final int TIMEOUT = Integer.MIN_VALUE;

    protected final SleepingStopwatch stopwatch;

    /**
     * The maximum number of stored permits.
     */
    protected double maxStoredPermits;

    /**
     * The time interval (in microseconds) between each permit
     */
    protected double permitIntervalMicros;

    /**
     * The currently stored permits.
     */
    protected double storedPermits;

    protected long nextPermitMicros;

    protected final Object mutex = new Object();

    public TokenBucketLimiter(RateLimitPolicy limitPolicy, SlidingWindow slidingWindow) {
        super(limitPolicy, TimeUnit.MILLISECONDS);
        this.stopwatch = SleepingStopwatch.createFromSystemTimer();
        this.nextPermitMicros = stopwatch.readMicros();
        this.permitIntervalMicros = slidingWindow.getPermitIntervalMicros();
        initialize();
        this.storedPermits = this.maxStoredPermits;
    }

    @Override
    protected boolean doAcquire(int permits, long timeout, TimeUnit timeUnit) {
        long timeoutMicros = timeout <= 0 ? 0 : timeUnit.toMicros(timeout);
        long startTimeMicros = stopwatch.readMicros();
        if (isTimeout(startTimeMicros, timeoutMicros) && isFull()) {
            return false;
        }
        return doAcquire(permits, startTimeMicros, timeoutMicros);
    }

    /**
     * Attempts to acquire the specified number of permits,
     * waiting if necessary until the permits become available or the specified timeout expires.
     *
     * @param permits          the number of permits to acquire
     * @param startTimeMicros  the request start time in microseconds.
     * @param timeoutMicros    the maximum time to wait in microseconds
     * @return true if the permits were acquired, false if the timeout expired
     */
    protected boolean doAcquire(int permits, long startTimeMicros, long timeoutMicros) {
        long microsToWait;
        synchronized (mutex) {
            // double check in lock
            if (isTimeout(startTimeMicros, timeoutMicros)) {
                return false;
            }
            microsToWait = waitForRequiredPermits(permits, startTimeMicros, timeoutMicros);
        }
        if (microsToWait == TIMEOUT) {
            return false;
        }
        stopwatch.sleepMicrosUninterruptibly(microsToWait);
        return true;
    }

    /**
     * Initializes the rate limiter by setting the maximum number of permits.
     */
    protected void initialize() {
        this.maxStoredPermits = Math.max(getMaxStoredPermits(), 1D);
    }

    /**
     * Calculates and returns the maximum number of permits that can be accumulated.
     *
     * @return the maximum number of permits
     */
    protected abstract double getMaxStoredPermits();

    /**
     * Checks if the current time is before the timeout time for acquiring a free ticket.
     *
     * @param startTimeMicros the start time in microseconds
     * @param timeoutMicros   the timeout time in microseconds
     * @return true if the current time is before the timeout time, false otherwise
     */
    protected boolean isTimeout(long startTimeMicros, long timeoutMicros) {
        return nextPermitMicros > startTimeMicros + timeoutMicros;
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
     * Estimates the wait time required to acquire the specified number of permits.
     *
     * @param permits         The number of permits to acquire.
     * @param startTimeMicros The request start time in microseconds.
     * @param timeoutMicros   The timeout time in microseconds
     * @return The estimated wait time in microseconds, or 0 if no wait is required.
     */
    protected long waitForRequiredPermits(long permits, long startTimeMicros, long timeoutMicros) {
        long nowMicros = stopwatch.readMicros();
        refresh(nowMicros);
        double available = min(permits, storedPermits);
        double lack = permits - available;
        long waitTimeMicros = waitForStorePermits(storedPermits, available) + (long) (lack * permitIntervalMicros);
        long momentAvailable = saturatedAdd(this.nextPermitMicros, waitTimeMicros);
        long microsToWait = max(momentAvailable - startTimeMicros, 0);
        if (microsToWait > timeoutMicros) {
            return TIMEOUT;
        }
        this.nextPermitMicros = momentAvailable;
        storedPermits -= available;
        return microsToWait;
    }

    /**
     * Adjusts the required wait time for acquiring permits based on the current time and the next token time.
     *
     * @param startTimeMicros The request start time in microseconds.
     * @param waitTimeMicros  The original wait time (in microseconds). This parameter is not used in the calculation.
     * @param nowMicros       The current time in microseconds
     * @param timeoutMicros   The timeout time in microseconds
     * @return The adjusted wait time (in microseconds), which is guaranteed to be non-negative.
     */
    protected long adjustWaitTime(long startTimeMicros, long timeoutMicros, long nowMicros, long waitTimeMicros) {
        return max(nextPermitMicros - startTimeMicros, 0);
    }

    /**
     * Waits for the specified number of stored permits to become available.
     *
     * @param storedPermits the current number of stored permits
     * @param targetPermits the number of permits to wait for
     * @return the time waited in microseconds
     */
    protected long waitForStorePermits(double storedPermits, double targetPermits) {
        return 0L;
    }

    /**
     * Returns the number of microseconds during cool down that we have to wait to get a new permit.
     */
    protected double coolDownIntervalMicros() {
        return permitIntervalMicros;
    }

    protected void refresh() {
        refresh(stopwatch.readMicros());
    }

    /**
     * Refresh permits based on the current time.
     *
     * @param nowMicros the current time in microseconds
     */
    protected void refresh(long nowMicros) {
        if (nowMicros > nextPermitMicros) {
            // if nextTokenMicros is in the past.
            double permits = (nowMicros - nextPermitMicros) / coolDownIntervalMicros();
            permits = storedPermits + permits;
            storedPermits = maxStoredPermits <= 0 ? permits : min(maxStoredPermits, permits);
            nextPermitMicros = nowMicros;
        }
    }

    /**
     * Converts the given time duration (in seconds) into the equivalent number of permits.
     *
     * @param seconds The time duration (in seconds) for which the permits are to be calculated. Must be a positive value.
     * @return The number of permits that can be acquired within the specified time duration.
     */
    protected double getPermits(long seconds) {
        return seconds * MICROSECOND_OF_ONE_SECOND / permitIntervalMicros;
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
