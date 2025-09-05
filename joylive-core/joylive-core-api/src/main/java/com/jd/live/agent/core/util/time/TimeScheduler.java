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

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static com.jd.live.agent.core.util.time.Timer.getRetryInterval;

/**
 * A TimeScheduler is a scheduler that manages timed tasks using a time wheel mechanism. It can schedule tasks to be
 * executed after a specified delay or at a scheduled time. The Timer is responsible for managing the lifecycle
 * of tasks, including their execution, cancellation, and any actions to be taken before or after running tasks.
 * It implements {@link AutoCloseable} to provide a mechanism to release resources when the timer is no longer needed.
 */
public class TimeScheduler implements AutoCloseable, Timer {

    /**
     * The prefix used for naming threads created by this timer.
     */
    private final String prefix;

    /**
     * The number of worker threads that execute expired tasks.
     */
    private final int workerThreads;

    /**
     * The maximum number of tasks that can be queued for execution at any given time.
     */
    private final long maxTasks;

    /**
     * A consumer that is called after a task has been executed.
     */
    private final Consumer<TimeWork> afterRun;

    /**
     * A consumer that is called after a task has been cancelled.
     */
    private final Consumer<TimeWork> afterCancel;

    /**
     * A consumer that is called before a task is executed.
     */
    private final Consumer<TimeWork> beforeRun;

    /**
     * The delay queue that holds time slots until they are ready to be processed.
     */
    private final DelayQueue<TimeSlot> queue;

    /**
     * The underlying time wheel that manages scheduling of tasks.
     */
    private final TimeWheel timeWheel;

    /**
     * The worker threads that execute expired tasks.
     */
    private List<Thread> workers;

    /**
     * The boos thread that poll the delay queue for expired tasks.
     */
    private Thread boss;

    /**
     * A queue of tasks that have been cancelled.
     */
    private final Queue<TimeWork> cancels = new ConcurrentLinkedQueue<>();

    /**
     * A queue of tasks that are pending to be scheduled onto the time wheel.
     */
    private final Queue<TimeWork> flying = new ConcurrentLinkedQueue<>();

    /**
     * A queue of tasks that are processing.
     */
    private final BlockingQueue<TimeWork> working = new LinkedBlockingQueue<>();

    /**
     * A count of the tasks that are currently pending execution.
     */
    private final AtomicLong tasks = new AtomicLong(0);

    /**
     * A flag indicating whether the timer has been started.
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Constructs a new Timer with the specified tick time, number of ticks, and number of worker threads.
     *
     * @param tickTime      The time in milliseconds that each tick represents.
     * @param ticks         The number of ticks in the time wheel.
     * @param workerThreads The number of worker threads to execute tasks.
     */
    public TimeScheduler(long tickTime, int ticks, int workerThreads) {
        this(null, tickTime, ticks, workerThreads, 0);
    }

    /**
     * Constructs a new Timer with the specified name, tick time, number of ticks, and number of worker threads.
     *
     * @param name          The name prefix for threads created by this timer.
     * @param tickTime      The time in milliseconds that each tick represents.
     * @param ticks         The number of ticks in the time wheel.
     * @param workerThreads The number of worker threads to execute tasks.
     */
    public TimeScheduler(String name, long tickTime, int ticks, int workerThreads) {
        this(name, tickTime, ticks, workerThreads, 0);
    }

    /**
     * Constructs a new Timer with the specified name, tick time, number of ticks, number of worker threads,
     * and maximum number of pending tasks.
     *
     * @param name          The name prefix for threads created by this timer.
     * @param tickTime      The time in milliseconds that each tick represents.
     * @param ticks         The number of ticks in the time wheel.
     * @param workerThreads The number of worker threads to execute tasks.
     * @param maxTasks      The maximum number of tasks that can be pending before being rejected.
     */
    public TimeScheduler(String name, long tickTime, int ticks, int workerThreads, long maxTasks) {
        if (tickTime <= 0) {
            throw new IllegalArgumentException("tickTime must be greater than 0");
        } else if (ticks <= 0) {
            throw new IllegalArgumentException("ticks must be greater than 0");
        } else if (workerThreads <= 0) {
            throw new IllegalArgumentException("workerThreads must be greater than 0");
        }
        this.prefix = name == null || name.isEmpty() ? "timer" : name;
        this.workerThreads = workerThreads;
        this.maxTasks = maxTasks;
        this.afterRun = o -> tasks.decrementAndGet();
        this.afterCancel = this::cancel;
        this.beforeRun = this::supply;
        this.queue = new DelayQueue<>();
        this.timeWheel = new TimeWheel(tickTime, ticks, System.currentTimeMillis(), queue);
    }

    /**
     * Starts the timer, initializing and starting the worker and boss thread pools.
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            this.workers = new ArrayList<>(workerThreads);
            // Use thread to avoid block by another apm agent which maybe enhance the thread pool.
            for (int i = 0; i < workerThreads; i++) {
                workers.add(startThread(prefix + "-worker-" + i, this::processWorking));
            }
            boss = startThread(prefix + "-boss", this::processQueue);
        }
    }

    @Override
    public void close() {
        if (started.compareAndSet(true, false)) {
            if (workers != null) {
                workers.forEach(Thread::interrupt);
            }
            if (boss != null) {
                boss.interrupt();
            }
        }
    }

    @Override
    public Timeout add(final String name, final long time, final Runnable runnable) {
        return runnable == null ? null : add(new TimeWork(name, timeWheel.getLeastOneTick(time), runnable, afterRun, afterCancel));
    }

    @Override
    public Timeout delay(final String name, final long delay, final Runnable runnable) {
        if (runnable == null) {
            return null;
        }
        long time = timeWheel.getDelayedLeastOneTick(delay);
        return add(new TimeWork(name, time, runnable, afterRun, afterCancel));
    }

    @Override
    public Timeout add(final TimeTask task) {
        if (task == null) {
            return null;
        }
        long time = task instanceof DelayTask ? timeWheel.getDelayedLeastOneTick(task.getTime()) : timeWheel.getLeastOneTick(task.getTime());
        return add(new TimeWork(task.getName(), time, task, afterRun, afterCancel));
    }

    @Override
    public void schedule(String name, long interval, long random, Runnable runnable) {
        if (runnable == null) {
            return;
        }
        delay(name, getRetryInterval(interval, random), () -> {
            try {
                runnable.run();
            } finally {
                if (started.get()) {
                    schedule(name, interval, random, runnable);
                }
            }
        });
    }

    /**
     * Starts and returns a new daemon thread with the given name.
     *
     * @param name     the name to assign to the new thread (used for identification)
     * @param runnable the task to be executed by the new thread
     * @return the started daemon thread instance
     */
    protected Thread startThread(String name, Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * Cancels pending tasks.
     */
    protected void cancel() {
        TimeWork timeWork;
        // Remove and cancel pending tasks
        while ((timeWork = cancels.poll()) != null) {
            timeWork.remove();
        }
    }

    /**
     * Supplies new tasks to the time wheel.
     * This method dequeues tasks from the 'flying' queue, which holds tasks pending to be scheduled,
     * and supplies them to the time wheel for execution at their designated times. It attempts to process
     * up to 100,000 tasks in one go, ensuring that a large number of tasks can be efficiently scheduled
     * without causing significant delays. Tasks that have been cancelled are skipped to ensure that only
     * valid tasks are scheduled.
     */
    protected void supply() {
        TimeWork timeWork;
        // Attempt to add tasks to the time wheel, with a maximum of 100,000 iterations
        // to prevent the method from running too long and potentially causing delays in scheduling.
        for (int i = 0; i < 100000; i++) {
            // Poll a task from the 'flying' queue, which contains tasks that are pending to be scheduled.
            timeWork = flying.poll();
            if (timeWork == null) {
                break;
            } else if (!timeWork.isCancelled()) {
                supply(timeWork);
            }
        }
    }

    /**
     * Supplies a single task to the time wheel.
     *
     * @param timeWork The task to be added.
     */
    protected void supply(final TimeWork timeWork) {
        if (!timeWheel.add(timeWork)) {
            working.add(timeWork);
        }
    }

    /**
     * Processes the time wheel queue in a loop while active.
     * <p>Polls for time slots, cancels expired tasks, supplies new tasks,
     * and advances the time wheel. Handles interruptions gracefully.
     */
    protected void processQueue() {
        while (started.get()) {
            try {
                // Wait for the next tick, polling for the next TimeSlot.
                TimeSlot timeSlot = queue.poll(timeWheel.tickTime, TimeUnit.MILLISECONDS);
                if (started.get()) {
                    // Process cancelled tasks and add new tasks.
                    cancel();
                    supply();
                    if (timeSlot != null) {
                        // Advance the time wheel and execute tasks in the current TimeSlot.
                        timeWheel.advance(timeSlot.expiration);
                        timeSlot.flush(beforeRun);
                    } else {
                        // Advance the time wheel by one tick in the absence of due tasks.
                        timeWheel.advance(timeWheel.now + timeWheel.tickTime);
                    }
                }
            } catch (InterruptedException e) {
                // Handle interruption gracefully by exiting the loop.
                break;
            }
        }
    }

    /**
     * Processes working tasks in a loop while active.
     * <p>Polls for tasks every second and executes them if available.
     * Handles interruptions gracefully.
     */
    protected void processWorking() {
        while (started.get()) {
            try {
                // Wait for task.
                TimeWork work = working.poll(1000L, TimeUnit.MILLISECONDS);
                if (started.get() && work != null) {
                    work.run();
                }
            } catch (InterruptedException e) {
                // Handle interruption gracefully by exiting the loop.
                break;
            }
        }
    }

    /**
     * Adds a task directly to the timer.
     *
     * @param timeWork The task to be added.
     * @return A Timeout object representing the scheduled task.
     */
    protected Timeout add(final TimeWork timeWork) {
        if (maxTasks > 0 && tasks.incrementAndGet() > maxTasks) {
            tasks.decrementAndGet();
            throw new RejectedExecutionException("the maximum of pending tasks is " + maxTasks);
        }
        flying.add(timeWork);
        return timeWork;
    }

    /**
     * Cancels a task.
     *
     * @param timeWork The task to be cancelled.
     */
    protected void cancel(final TimeWork timeWork) {
        tasks.decrementAndGet();
        cancels.add(timeWork);
    }

}
