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
package com.jd.live.agent.core.util.time;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * The Timer interface defines the contract for a timing mechanism that can schedule tasks
 * for future execution in a background thread. The tasks can be scheduled for one-time
 * execution, or for repeated execution at regular intervals.
 */
public interface Timer {

    String COMPONENT_TIMER = "timer";

    /**
     * Adds a task to be executed at least one tick in the future.
     *
     * @param name     The name of the task.
     * @param time     The absolute execution time for the task.
     * @param runnable The task to be executed.
     * @return A Timeout object representing the scheduled task.
     */
    Timeout add(String name, long time, Runnable runnable);

    /**
     * Adds a task to be executed after a specified delay.
     *
     * @param name     The name of the task.
     * @param delay    The delay in milliseconds before the task should be executed.
     * @param runnable The task to be executed.
     * @return A Timeout object representing the scheduled task.
     */
    Timeout delay(String name, long delay, Runnable runnable);

    /**
     * Adds a task that needs to be executed at least one tick in the future.
     *
     * @param task The timed task to be added.
     * @return A Timeout object representing the scheduled task.
     */
    Timeout add(TimeTask task);

    /**
     * Schedules a task with specified interval.
     *
     * @param name     task name
     * @param interval delay in milliseconds
     * @param runnable task to execute
     */
    default void schedule(String name, long interval, Runnable runnable) {
        schedule(name, interval, 0, runnable, null);
    }

    /**
     * Schedules a task with specified interval and random jitter.
     *
     * @param name     task name
     * @param interval base delay in milliseconds
     * @param random   additional random delay in milliseconds
     * @param runnable task to execute
     */
    default void schedule(String name, long interval, long random, Runnable runnable) {
        schedule(name, interval, random, runnable, null);
    }

    /**
     * Schedules a task with interval and random delay.
     *
     * @param name      task identifier
     * @param interval  base delay (ms)
     * @param random    max random delay (ms)
     * @param runnable  task to run
     * @param condition execution condition
     */
    void schedule(String name, long interval, long random, Runnable runnable, Supplier<Boolean> condition);

    /**
     * Calculates actual retry interval with optional random jitter.
     *
     * @param interval base interval in ms
     * @param random   maximum random jitter to add (0 for no jitter)
     * @return calculated interval (base + random jitter if applicable)
     */
    static long getRetryInterval(long interval, long random) {
        return random <= 0 ? interval : (interval + ThreadLocalRandom.current().nextLong(random));
    }
}
