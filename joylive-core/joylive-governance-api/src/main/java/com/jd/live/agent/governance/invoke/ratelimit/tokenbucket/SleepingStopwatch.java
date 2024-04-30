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

public abstract class SleepingStopwatch {

    /**
     * Constructor for use by subclasses.
     */
    protected SleepingStopwatch() {
    }

    /*
     * We always hold the mutex when calling this. TODO(cpovirk): Is that important? Perhaps we need
     * to guarantee that each call to reserveEarliestAvailable, etc. sees a value >= the previous?
     * Also, is it OK that we don't hold the mutex when sleeping?
     */
    protected abstract long readMicros();

    protected abstract void sleepMicrosUninterruptibly(long micros);

    public static SleepingStopwatch createFromSystemTimer() {
        return new SleepingStopwatch() {
            private final Ticker ticker = Ticker.systemTicker();
            private final long startTick = ticker.read();

            @Override
            protected long readMicros() {
                return TimeUnit.MICROSECONDS.convert(elapsedNanos(), TimeUnit.NANOSECONDS);
            }

            @Override
            protected void sleepMicrosUninterruptibly(long micros) {
                if (micros > 0) {
                    sleepUninterruptibly(micros, TimeUnit.MICROSECONDS);
                }
            }

            private long elapsedNanos() {
                return ticker.read() - startTick;
            }

        };
    }

    public static void sleepUninterruptibly(long sleepFor, TimeUnit unit) {
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

    static abstract class Ticker {
        /**
         * Constructor for use by subclasses.
         */
        protected Ticker() {
        }

        /**
         * Returns the number of nanoseconds elapsed since this ticker's fixed point of reference.
         */
        public abstract long read();

        /**
         * A ticker that reads the current time using {@link System#nanoTime}.
         *
         * @since 10.0
         */
        public static Ticker systemTicker() {
            return SYSTEM_TICKER;
        }

        private static final Ticker SYSTEM_TICKER =
                new Ticker() {
                    @Override
                    public long read() {
                        return System.nanoTime();
                    }
                };
    }
}