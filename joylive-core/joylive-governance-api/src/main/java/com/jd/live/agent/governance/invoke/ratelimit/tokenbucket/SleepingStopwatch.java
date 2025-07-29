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

import java.util.concurrent.TimeUnit;

/**
 * An interface for a stopwatch that can also sleep uninterruptedly.
 * <p>
 * Source code implementation borrows from com.google.common.util.concurrent.RateLimiter.SleepingStopwatch
 * </p>
 */
public interface SleepingStopwatch {

    /**
     * Returns the number of microseconds elapsed since this stopwatch was started.
     *
     * @return the number of microseconds elapsed
     */
    long readMicros();

    /**
     * Sleeps uninterruptedly for the specified number of microseconds.
     *
     * @param micros the number of microseconds to sleep
     */
    void sleepMicrosUninterruptibly(long micros);

    /**
     * Creates a new instance of SleepingStopwatch using the system timer.
     *
     * @return a new instance of SleepingStopwatch
     */
    static SleepingStopwatch createFromSystemTimer() {
        // Used to get the time interval, so a new instance needs to be created.
        return new SleepingStopwatch() {
            private final Ticker ticker = Ticker.systemTicker();
            private final long startTick = ticker.read();

            @Override
            public long readMicros() {
                return TimeUnit.MICROSECONDS.convert(ticker.read() - startTick, TimeUnit.NANOSECONDS);
            }

            @Override
            public void sleepMicrosUninterruptibly(long micros) {
                if (micros >= 5) {
                    // skip small sleeps to avoid thread wake-up overhead
                    sleepUninterruptibly(micros, TimeUnit.MICROSECONDS);
                }
            }
        };
    }

    /**
     * Sleeps uninterruptedly for the specified amount of time.
     *
     * @param sleepFor the amount of time to sleep
     * @param unit     the unit of time for sleepFor
     */
    static void sleepUninterruptibly(long sleepFor, TimeUnit unit) {
        boolean interrupted = false;
        try {
            long remainingNanos = unit.toNanos(sleepFor);
            long end = System.nanoTime() + remainingNanos;
            while (true) {
                try {
                    // TimeUnit.sleep() treats negative timeouts just like zero.
                    TimeUnit.NANOSECONDS.sleep(remainingNanos);
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                    remainingNanos = end - System.nanoTime();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * A nested interface for a ticker that returns the number of nanoseconds elapsed since a fixed point of reference.
     */
    interface Ticker {

        /**
         * Returns the number of nanoseconds elapsed since this ticker's fixed point of reference.
         */
        long read();

        /**
         * A ticker that reads the current time using {@link System#nanoTime}.
         *
         * @since 10.0
         */
        static Ticker systemTicker() {
            return SYSTEM_TICKER;
        }

        Ticker SYSTEM_TICKER = System::nanoTime;
    }

}