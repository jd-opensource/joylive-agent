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
package com.jd.live.agent.plugin.application.springboot.v2.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The {@code AppCounter} class provides a mechanism to track and manage application lifecycle events
 * using counters. It allows executing runnable tasks when specific events are triggered, ensuring
 * that tasks are executed only once per event.
 */
public class AppLifecycle {

    public static ThreadLocal<Boolean> IGNORED = new ThreadLocal<>();

    /**
     * Event key representing the application loading event.
     */
    public static final String EVENT_LOAD = "onLoading";

    /**
     * Event key representing the event when the application environment is prepared.
     */
    public static final String EVENT_ON_ENVIRONMENT_PREPARED = "onEnvironmentPrepared";

    /**
     * Event key representing the event when the application has started.
     */
    public static final String EVENT_ON_STARTED = "onStarted";

    /**
     * Event key representing the event when the application is ready.
     */
    public static final String EVENT_ON_READY = "onReady";

    /**
     * Event key representing the event when the application is stopping.
     */
    public static final String EVENT_ON_STOP = "onStop";

    /**
     * A concurrent map to store counters for each event key.
     */
    private static final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    /**
     * Increments the counter for the specified key and returns the new value.
     * If the counter does not exist, it is initialized to 0 before incrementing.
     *
     * @param key the key for which to increment the counter
     * @return the incremented value of the counter
     */
    private static long incrementAndGet(String key) {
        AtomicLong counter = counters.computeIfAbsent(key, k -> new AtomicLong(0));
        return counter.incrementAndGet();
    }

    /**
     * Retrieves the current value of the counter for the specified key.
     * If the counter does not exist, returns 0.
     *
     * @param key the key for which to retrieve the counter value
     * @return the current value of the counter, or 0 if the counter does not exist
     */
    private static long get(String key) {
        AtomicLong counter = counters.get(key);
        return counter == null ? 0 : counter.get();
    }

    /**
     * Increments the counter for the specified key and checks if it matches the value of the
     * {@link #EVENT_LOAD} counter. This method is used to determine if a specific event should
     * trigger an action.
     *
     * @param key the key for which to increment and check the counter
     * @return {@code true} if the counter value matches the {@link #EVENT_LOAD} counter and the
     * counter is greater than 0, otherwise {@code false}
     */
    private static boolean enter(String key) {
        long v1 = incrementAndGet(key);
        long v2 = get(EVENT_LOAD);
        return v1 == v2 && v2 > 0;
    }

    /**
     * Executes the provided {@code Runnable} task if the {@link #enter(String)} method returns
     * {@code true} for the specified key.
     *
     * @param key      the key for which to check the counter
     * @param runnable the task to execute if the condition is met
     */
    private static void enter(String key, Runnable runnable) {
        if (enter(key) && runnable != null) {
            runnable.run();
        }
    }

    /**
     * Increments the {@link #EVENT_LOAD} counter and checks if it was previously 0.
     * This method is used to determine if the application loading event is being triggered
     * for the first time.
     *
     * @return {@code true} if the {@link #EVENT_LOAD} counter was previously 0, otherwise
     * {@code false}
     */
    private static boolean load() {
        // fix for sofa boot
        Boolean ignored = IGNORED.get();
        if (ignored == null || !ignored) {
            return incrementAndGet(EVENT_LOAD) == 1;
        }
        return false;
    }

    /**
     * Executes the provided {@code Runnable} task if the {@link #load()} method returns
     * {@code true}. This ensures that the task is executed only once during the application
     * loading event.
     *
     * @param runnable the task to execute if the application loading event is triggered for
     *                 the first time
     */
    public static void load(Runnable runnable) {
        if (load() && runnable != null) {
            runnable.run();
        }
    }

    /**
     * Executes the provided {@code Runnable} task when the application environment is prepared.
     * The task is executed only if the {@link #enter(String)} method returns {@code true} for
     * the {@link #EVENT_ON_ENVIRONMENT_PREPARED} event.
     *
     * @param runnable the task to execute when the application environment is prepared
     */
    public static void prepared(Runnable runnable) {
        enter(EVENT_ON_ENVIRONMENT_PREPARED, runnable);
    }

    /**
     * Executes the provided {@code Runnable} task when the application has started.
     * The task is executed only if the {@link #enter(String)} method returns {@code true} for
     * the {@link #EVENT_ON_STARTED} event.
     *
     * @param runnable the task to execute when the application has started
     */
    public static void started(Runnable runnable) {
        enter(EVENT_ON_STARTED, runnable);
    }

    /**
     * Executes the provided {@code Runnable} task when the application is ready.
     * The task is executed only if the {@link #enter(String)} method returns {@code true} for
     * the {@link #EVENT_ON_READY} event.
     *
     * @param runnable the task to execute when the application is ready
     */
    public static void ready(Runnable runnable) {
        enter(EVENT_ON_READY, runnable);
    }
}

