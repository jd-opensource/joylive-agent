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
package com.jd.live.agent.core.util;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A Waiter interface that encapsulates various waiting mechanisms. It defines methods for waiting with specified time,
 * time units, and conditions. Implementing classes can provide specific logic for waiting and waking up operations.
 */
public interface Waiter {

    /**
     * Waits for a specified amount of time and time unit. This default implementation calls another await method with
     * the given time, time unit, and null for the condition.
     *
     * @param time     The time to wait.
     * @param timeUnit The unit of time.
     * @throws InterruptedException if any thread has interrupted the current thread.
     */
    default void await(long time, TimeUnit timeUnit) throws InterruptedException {
        await(time, timeUnit, null);
    }

    /**
     * Waits according to the specifications of a Waiting object, if not null. This default method extracts the time,
     * time unit, and condition from the Waiting object and calls another await method.
     *
     * @param waiting The Waiting object containing wait specifications.
     * @throws InterruptedException if any thread has interrupted the current thread.
     */
    default void await(Waiting waiting) throws InterruptedException {
        if (waiting != null) {
            await(waiting.time, waiting.timeUnit, waiting.condition);
        }
    }

    /**
     * Waits for a specified amount of time, time unit, and an optional condition. This is an abstract method that needs
     * to be implemented by concrete classes.
     *
     * @param time      The time to wait.
     * @param timeUnit  The unit of time.
     * @param condition An optional condition that determines if the wait should occur.
     * @throws InterruptedException if any thread has interrupted the current thread.
     */
    void await(long time, TimeUnit timeUnit, Supplier<Boolean> condition) throws InterruptedException;

    /**
     * Wakes up the waiting thread or operation. This is an abstract method that needs to be implemented by concrete classes.
     */
    void wakeup();

    /**
     * A Waiter implementation that uses an object's monitor (synchronized blocks and wait/notifyAll methods) for waiting and waking up.
     */
    class MutexWaiter implements Waiter {

        protected final Object mutex;

        /**
         * Default constructor initializing with a new mutex object.
         */
        public MutexWaiter() {
            this(new Object());
        }

        /**
         * Constructor with a custom mutex object.
         *
         * @param mutex The mutex object to be used for synchronization.
         */
        public MutexWaiter(Object mutex) {
            this.mutex = mutex == null ? new Object() : mutex;
        }

        @Override
        public void await(long time, TimeUnit timeUnit, Supplier<Boolean> condition) throws InterruptedException {
            synchronized (mutex) {
                if (condition == null || condition.get()) {
                    mutex.wait(timeUnit.toMillis(time));
                }
            }
        }

        @Override
        public void wakeup() {
            synchronized (mutex) {
                mutex.notifyAll();
            }
        }

    }

    /**
     * A Waiter implementation that uses thread sleep for waiting. Wakeup method in this implementation is not applicable.
     */
    class SleepWaiter implements Waiter {

        @Override
        public void await(long time, TimeUnit timeUnit, Supplier<Boolean> condition) throws InterruptedException {
            if (condition == null || condition.get()) {
                Thread.sleep(timeUnit.toMillis(time));
            }
        }

        @Override
        public void wakeup() {
            // Not applicable for sleep-based waiting.
        }

    }

    /**
     * A class representing waiting specifications including time, time unit, and an optional condition.
     */
    final class Waiting {
        private final long time;
        private final TimeUnit timeUnit;
        private final Supplier<Boolean> condition;

        public Waiting(long time) {
            this(time, TimeUnit.MILLISECONDS, null);
        }

        public Waiting(long time, Supplier<Boolean> condition) {
            this(time, TimeUnit.MILLISECONDS, condition);
        }

        public Waiting(long time, TimeUnit timeUnit, Supplier<Boolean> condition) {
            this.time = time;
            this.timeUnit = timeUnit == null ? TimeUnit.MILLISECONDS : timeUnit;
            this.condition = condition;
        }

        public long getTime() {
            return time;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        public Supplier<Boolean> getCondition() {
            return condition;
        }
    }
}

