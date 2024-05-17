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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;

/**
 * Represents a scheduled task that can be executed at a specified time, cancelled, or checked for expiration.
 */
public class TimeWork implements Runnable, Timeout {
    /**
     * Initial state indicating the task has not been executed or cancelled.
     */
    protected static final int INIT = 0;

    /**
     * State indicating the task has been cancelled.
     */
    protected static final int CANCELLED = 1;

    /**
     * State indicating the task has been executed or has expired.
     */
    protected static final int EXPIRED = 2;

    /**
     * Atomic updater to safely update the state of the task across multiple threads.
     */
    protected static final AtomicIntegerFieldUpdater<TimeWork> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(TimeWork.class, "state");

    /**
     * The name of the task for identification purposes.
     */
    private final String name;

    /**
     * The scheduled execution time of the task.
     */
    protected final long time;

    /**
     * The {@code Runnable} task to be executed.
     */
    private final Runnable runnable;

    /**
     * A consumer that is called after the task has been run.
     */
    private final Consumer<TimeWork> afterRun;

    /**
     * A consumer that is called if the task is cancelled.
     */
    private final Consumer<TimeWork> afterCancel;

    /**
     * The {@code TimeSlot} in which this task is scheduled. Used for managing task execution timing.
     */
    protected TimeSlot timeSlot;

    /**
     * Reference to the next {@code TimeWork} node in the linked list, if any.
     */
    protected TimeWork next;

    /**
     * Reference to the previous {@code TimeWork} node in the linked list, if any.
     */
    protected TimeWork pre;

    /**
     * The current state of the task, indicating whether it is initialized, cancelled, or expired.
     */
    protected volatile int state = INIT;

    /**
     * Constructs a new {@code TimeWork} instance with the specified properties.
     *
     * @param name        The name of the task.
     * @param time        The scheduled execution time of the task.
     * @param runnable    The {@code Runnable} task to be executed.
     * @param afterRun    A consumer that is called after the task has been run.
     * @param afterCancel A consumer that is called if the task is cancelled.
     */
    public TimeWork(final String name, final long time, final Runnable runnable,
                    final Consumer<TimeWork> afterRun,
                    final Consumer<TimeWork> afterCancel) {
        this.time = time;
        this.name = name;
        this.runnable = runnable;
        this.afterRun = afterRun;
        this.afterCancel = afterCancel;
        this.timeSlot = null;
        this.next = null;
        this.pre = null;
    }

    @Override
    public String toString() {
        return name == null || name.isEmpty() ? super.toString() : name;
    }

    @Override
    public void run() {
        if (STATE_UPDATER.compareAndSet(this, INIT, EXPIRED)) {
            runnable.run();
            if (afterRun != null) {
                afterRun.accept(this);
            }
        }
    }

    @Override
    public boolean isExpired() {
        return state == EXPIRED;
    }

    @Override
    public boolean isCancelled() {
        return state == CANCELLED;
    }

    @Override
    public boolean cancel() {
        if (STATE_UPDATER.compareAndSet(this, INIT, CANCELLED)) {
            if (afterCancel != null) {
                afterCancel.accept(this);
            }
            return true;
        }
        return false;
    }

    /**
     * Removes this task from its associated {@code TimeSlot}, if any.
     */
    void remove() {
        if (timeSlot != null) {
            timeSlot.remove(this);
        }
    }
}

