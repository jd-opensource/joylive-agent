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

import java.util.concurrent.DelayQueue;

/**
 * A time wheel is a data structure used for managing tasks that are to be executed after a certain delay.
 * It consists of a circular list of time slots and provides efficient means to schedule tasks, advance time,
 * and trigger task execution. This is particularly useful for implementing timer mechanisms in applications
 * like schedulers, task managers, or any system that needs to handle timed events.
 */
public class TimeWheel {
    /**
     * The duration of a single tick in the time wheel.
     */
    protected final long tickTime;

    /**
     * The number of ticks in the time wheel.
     */
    private final int ticks;

    /**
     * The total duration covered by the time wheel.
     */
    private final long duration;

    /**
     * The current time, which is a multiple of {@code tickTime}.
     */
    protected long now;

    /**
     * The current position in the time slots array.
     */
    private int index;

    /**
     * The delay queue used to manage and trigger the execution of tasks when their delay has passed.
     */
    private final DelayQueue<TimeSlot> queue;

    /**
     * An array of time slots that hold the tasks.
     */
    private final TimeSlot[] timeSlots;

    /**
     * A reference to the next layer of the time wheel, which is used for scheduling tasks beyond the current time wheel's duration.
     */
    private TimeWheel next;

    /**
     * Constructs a new {@code TimeWheel} instance with the specified parameters.
     *
     * @param tickTime The duration of each tick in milliseconds.
     * @param ticks    The number of ticks the time wheel should have.
     * @param now      The current time in milliseconds.
     * @param queue    The delay queue used for managing time slots.
     */
    public TimeWheel(final long tickTime, final int ticks, final long now, final DelayQueue<TimeSlot> queue) {
        this.tickTime = tickTime;
        this.ticks = ticks;
        this.duration = ticks * tickTime;
        this.timeSlots = new TimeSlot[ticks];
        // Align the current time to be a multiple of tickTime
        this.now = now - (now % tickTime);
        this.queue = queue;
        for (int i = 0; i < ticks; i++) {
            timeSlots[i] = new TimeSlot();
        }
    }

    /**
     * Creates or retrieves the next layer of the time wheel.
     *
     * @return The next layer of the time wheel.
     */
    protected TimeWheel getNext() {
        if (next == null) {
            next = new TimeWheel(duration, ticks, now, queue);
        }
        return next;
    }

    /**
     * Calculates the time point at least one tick ahead of the current time.
     *
     * @param time The reference time.
     * @return The time point at least one tick in the future.
     */
    public long getLeastOneTick(final long time) {
        long result = System.currentTimeMillis() + tickTime;
        return Math.max(time, result);
    }

    /**
     * Adds a task to the appropriate time slot in the time wheel.
     *
     * @param timeWork The task to be scheduled.
     * @return {@code true} if the task was successfully added, {@code false} if the task is already expired.
     */
    public boolean add(final TimeWork timeWork) {
        long time = timeWork.time - now;
        if (time < tickTime) {
            // If the task is expired, it should be executed immediately and not added to the time wheel
            return false;
        } else if (time < duration) {
            // If the task falls within this time wheel's duration, add it to the appropriate time slot
            int count = (int) (time / tickTime);
            TimeSlot timeSlot = timeSlots[(count + index) % ticks];
            // Add the task to the slot
            if (timeSlot.add(timeWork, now + count * tickTime) == TimeSlot.HEAD) {
                queue.offer(timeSlot);
            }
            return true;
        } else {
            // If the task is beyond this time wheel's duration, add it to the next layer
            return getNext().add(timeWork);
        }
    }

    /**
     * Advances the time wheel to a new timestamp, potentially triggering the execution of tasks that have reached their expiration.
     *
     * @param timestamp The new timestamp to advance to.
     */
    public void advance(final long timestamp) {
        if (timestamp >= now + tickTime) {
            now = timestamp - (timestamp % tickTime);
            index++;
            if (index >= ticks) {
                index = 0;
            }
            if (next != null) {
                // Advance the time in the next layer of the time wheel
                next.advance(timestamp);
            }
        }
    }
}

