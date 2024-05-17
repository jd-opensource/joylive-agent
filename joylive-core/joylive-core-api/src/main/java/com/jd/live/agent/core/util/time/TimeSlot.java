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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Represents a time slot within a timing wheel or similar scheduling system.
 * A time slot is responsible for holding and managing {@link TimeWork} tasks that are scheduled to be executed
 * at a specific expiration time. This class provides mechanisms to add, remove, and flush tasks when their
 * execution time has arrived.
 */
public class TimeSlot implements Delayed {

    /**
     * Indicates that the task is being added at the head of the list within the slot.
     */
    public static final int HEAD = 1;

    /**
     * Indicates that the task is being added at the tail of the list within the slot.
     */
    public static final int TAIL = 2;

    /**
     * The expiration time of the time slot. Tasks within this slot are due to be executed when the current
     * time surpasses this expiration time.
     */
    protected long expiration = -1L;

    /**
     * The root node of a doubly linked list that holds the tasks. This is a sentinel node to simplify the
     * add and remove operations by eliminating the need to check for null.
     */
    private final TimeWork root = new TimeWork("root", -1L, null, null, null);

    /**
     * Constructs a new {@code TimeSlot} instance. Initializes the doubly linked list with the root node
     * pointing to itself, indicating an empty list.
     */
    public TimeSlot() {
        root.pre = root;
        root.next = root;
    }

    /**
     * Adds a new {@link TimeWork} task to this time slot and sets a new expiration time for the slot if necessary.
     *
     * @param timeWork The task to be added.
     * @param expire   The new expiration time for the slot.
     * @return An integer indicating whether the task was added at the head ({@link #HEAD}) or tail ({@link #TAIL}) of the list.
     */
    protected int add(final TimeWork timeWork, final long expire) {
        timeWork.timeSlot = this;
        TimeWork tail = root.pre;
        timeWork.next = root;
        timeWork.pre = tail;
        tail.next = timeWork;
        root.pre = timeWork;
        if (expiration == -1L) {
            expiration = expire;
            return HEAD;
        }
        return TAIL;
    }

    /**
     * Removes a {@link TimeWork} task from this time slot. This operation effectively detaches the task from
     * the doubly linked list within the slot.
     *
     * @param timeWork The task to be removed.
     */
    protected void remove(final TimeWork timeWork) {
        timeWork.next.pre = timeWork.pre;
        timeWork.pre.next = timeWork.next;
        timeWork.timeSlot = null;
        timeWork.next = null;
        timeWork.pre = null;
    }

    /**
     * Flushes this time slot by executing all tasks within it. This method is called when the time slot has
     * expired. Each task is removed from the slot and then passed to the provided consumer for execution.
     *
     * @param consumer A {@link Consumer} that takes a {@link TimeWork} task and executes it.
     */
    protected void flush(final Consumer<TimeWork> consumer) {
        List<TimeWork> ts = new LinkedList<>();
        TimeWork timeWork = root.next;
        while (timeWork != root) {
            remove(timeWork);
            ts.add(timeWork);
            timeWork = root.next;
        }
        expiration = -1L;
        ts.forEach(consumer);
    }

    @Override
    public long getDelay(final TimeUnit unit) {
        long delayMs = expiration - System.currentTimeMillis();
        return Math.max(0, unit.convert(delayMs, TimeUnit.MILLISECONDS));
    }

    @Override
    public int compareTo(final Delayed o) {
        return o instanceof TimeSlot ? Long.compare(expiration, ((TimeSlot) o).expiration) : 0;
    }
}

