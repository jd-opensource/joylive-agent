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

import com.jd.live.agent.core.util.Waiter.Waiting;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A class that implements a daemon thread for executing tasks periodically or based on dynamic scheduling.
 * It is designed with flexibility to support initialization steps, custom execution logic, error handling,
 * conditional continuation, and graceful shutdown.
 */
public class Daemon implements AutoCloseable {

    /**
     * The name of the daemon thread.
     */
    protected String name;
    /**
     * A {@link Runnable} representing the preparation logic to be executed before the main task.
     */
    protected Runnable prepare;
    /**
     * A {@link Callable} object that defines the main execution logic of the daemon.
     * It returns a {@link Waiting} object specifying the next wait time.
     */
    protected Callable<Waiting> callable;
    /**
     * A {@link Consumer} that handles any {@link Throwable} exceptions thrown during execution.
     */
    protected Consumer<Throwable> error;
    /**
     * The initial delay time before the first execution of the task.
     */
    protected long delay;
    /**
     * The fault tolerance time to wait after an error has occurred before retrying the task.
     */
    protected long fault;
    /**
     * A {@link Supplier} that provides a Boolean indicating whether the daemon should continue executing.
     */
    protected Supplier<Boolean> condition;
    /**
     * A {@link Waiter} object used for managing wait times between executions.
     */
    protected Waiter waiter;
    /**
     * The {@link Thread} on which the daemon runs.
     */
    protected Thread thread;
    /**
     * An {@link AtomicBoolean} flag indicating whether the daemon has been started.
     */
    protected AtomicBoolean started = new AtomicBoolean();

    /**
     * Constructs a new {@code Daemon} instance with fixed interval execution.
     *
     * @param name      The name of the daemon thread.
     * @param prepare   The preparation logic to execute before the main task.
     * @param runnable  The main execution logic as a {@link Runnable}.
     * @param interval  The fixed interval between task executions.
     * @param delay     The initial delay before the first execution.
     * @param error     The error handling logic.
     * @param fault     The fault tolerance time after an error.
     * @param condition The condition under which the daemon should continue executing.
     * @param waiter    The {@link Waiter} object for managing wait times.
     */
    public Daemon(final String name, final Runnable prepare, final Runnable runnable, final long interval, final long delay,
                  final Consumer<Throwable> error, final long fault,
                  final Supplier<Boolean> condition, final Waiter waiter) {
        this(name, prepare,
                () -> {
                    runnable.run();
                    return new Waiting(interval);
                },
                delay, error, fault, condition, waiter);
    }

    /**
     * Constructs a new {@code Daemon} instance with dynamic scheduling.
     *
     * @param name      The name of the daemon thread.
     * @param prepare   The preparation logic to execute before the main task.
     * @param callable  The main execution logic as a {@link Callable} returning a {@link Waiting} object.
     * @param delay     The initial delay before the first execution.
     * @param error     The error handling logic.
     * @param fault     The fault tolerance time after an error.
     * @param condition The condition under which the daemon should continue executing.
     * @param waiter    The {@link Waiter} object for managing wait times.
     */
    public Daemon(final String name, final Runnable prepare, final Callable<Waiting> callable, final long delay,
                  final Consumer<Throwable> error, final long fault,
                  final Supplier<Boolean> condition,
                  final Waiter waiter) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name can not be empty");
        } else if (callable == null) {
            throw new IllegalArgumentException("callable can not be null");
        }
        this.name = name;
        this.prepare = prepare;
        this.callable = callable;
        this.error = error;
        this.delay = delay;
        this.fault = fault;
        this.condition = condition;
        this.waiter = waiter == null ? new Waiter.SleepWaiter() : waiter;
    }

    /**
     * Checks whether the daemon has been started.
     *
     * @return {@code true} if the daemon has been started, otherwise {@code false}.
     */
    public boolean isStarted() {
        return started.get();
    }

    /**
     * Starts the daemon thread.
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            thread = new Thread(() -> {
                if (prepare != null) {
                    prepare.run();
                }
                Waiting waiting = new Waiting(delay);
                while (continuous()) {
                    try {
                        if (waiting.getTime() > 0) {
                            waiter.await(waiting);
                            if (continuous()) {
                                waiting = callable.call();
                            }
                        } else {
                            waiting = callable.call();
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Throwable e) {
                        if (error != null) {
                            error.accept(e);
                        }
                        waiting = new Waiting(fault);
                    }
                }
            }, name);
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * Determines whether the daemon should continue executing.
     *
     * @return {@code true} if the daemon should continue, otherwise {@code false}.
     */
    protected boolean continuous() {
        return started.get() && (condition == null || condition.get());
    }

    /**
     * Stops the daemon thread.
     */
    public void stop() {
        if (started.compareAndSet(true, false)) {
            waiter.wakeup();
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
        }
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * Creates and returns a new {@link Builder} for constructing a {@code Daemon} instance.
     *
     * @return A new {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder class for constructing instances of {@code Daemon} with customized configurations.
     */
    public static final class Builder {
        private String name;
        private Runnable prepare;
        private Runnable runnable;
        private Callable<Waiting> callable;
        private Consumer<Throwable> error;
        private Long delay;
        private Long interval;
        private Long fault;
        private Supplier<Boolean> condition;
        private Waiter waiter;

        public Builder() {
        }

        public Builder name(String val) {
            name = val;
            return this;
        }

        public Builder prepare(Runnable val) {
            prepare = val;
            return this;
        }

        public Builder runnable(Runnable val) {
            runnable = val;
            return this;
        }

        public Builder callable(Callable<Waiting> val) {
            callable = val;
            return this;
        }

        public Builder error(Consumer<Throwable> val) {
            error = val;
            return this;
        }

        public Builder delay(long val) {
            delay = val;
            return this;
        }

        public Builder interval(long val) {
            interval = val;
            if (fault == null) {
                fault = val;
            }
            if (delay == null) {
                delay = 0L;
            }
            return this;
        }

        public Builder fault(long val) {
            fault = val;
            return this;
        }

        public Builder condition(Supplier<Boolean> val) {
            condition = val;
            return this;
        }

        public Builder waiter(Waiter val) {
            waiter = val;
            return this;
        }

        public Daemon build() {
            return callable != null ? new Daemon(name, prepare, callable, delay == null ? 0 : delay, error, fault == null ? 0 : fault, condition, waiter) :
                    new Daemon(name, prepare, runnable, interval == null ? 0 : interval, delay == null ? 0 : delay, error, fault == null ? 0 : fault, condition, waiter);
        }
    }
}
