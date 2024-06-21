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
package com.jd.live.agent.core.util.shutdown;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the orderly shutdown of application components, ensuring that all registered
 * {@link ShutdownHook} instances are executed within a specified timeout period.
 * <p>
 * This class allows for the graceful shutdown of resources by registering {@code ShutdownHook}
 * instances which can be executed when the application is shutting down. Hooks can be added
 * at any time before the shutdown process begins. Once the shutdown process starts, no new hooks
 * can be added.
 * </p>
 */
public class Shutdown {

    private static final Logger logger = LoggerFactory.getLogger(Shutdown.class);

    private static final int DEFAULT_TIMEOUT = 10 * 1000;

    private final List<ShutdownHook> hooks = new CopyOnWriteArrayList<>();

    private final AtomicBoolean register = new AtomicBoolean();

    private final AtomicBoolean shutdown = new AtomicBoolean();

    private final long timeout;

    private final Thread shutdownTask;

    public Shutdown() {
        this(DEFAULT_TIMEOUT);
    }

    public Shutdown(long timeout) {
        this.timeout = timeout;
        this.shutdownTask = new Thread(() -> {
            try {
                doShutdown().get(this.timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException ignored) {
            }
        }, "LiveAgent-Shutdown");
    }

    /**
     * Initiates the shutdown process, executing all registered {@code ShutdownHook} instances
     * within the specified timeout period.
     * <p>
     * This method is synchronized to ensure that the shutdown process is only executed once.
     * If the shutdown process has already started, this method returns immediately with a
     * completed future.
     * </p>
     *
     * @return a {@link CompletableFuture} representing the pending completion of the shutdown process
     */
    private synchronized CompletableFuture<Void> doShutdown() {
        if (!shutdown.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }
        logger.info("LiveAgent shutdown....");
        CompletableFuture<Void> result = null;
        if (!hooks.isEmpty()) {
            List<ShutdownHookGroup> groups = sortGroup();

            // Sequentially execute hooks
            for (ShutdownHookGroup group : groups) {
                if (result == null) {
                    result = group.run();
                } else {
                    result = result.handle((t, r) -> group.run().join());
                }
            }
        }
        result = result == null ? CompletableFuture.completedFuture(null) : result;
        return result.whenComplete((r, t) -> {
            if (t == null) {
                logger.info("LiveAgent shutdown successfully.");
            } else {
                logger.error("LiveAgent shutdown failed.", t);
            }
        });
    }

    /**
     * Groups and sorts the registered shutdown hooks by their priority, preparing them for execution.
     *
     * @return a list of {@link ShutdownHookGroup} instances, each containing hooks of the same priority
     */
    private List<ShutdownHookGroup> sortGroup() {
        // Group hook by priority
        List<ShutdownHookGroup> groups = new ArrayList<>();

        // Sort hook by priority.
        hooks.sort(Comparator.comparingInt(ShutdownHook::priority));
        ShutdownHookGroup lastGroup = null;

        for (ShutdownHook hook : hooks) {
            if (lastGroup == null || lastGroup.priority != hook.priority()) {
                lastGroup = new ShutdownHookGroup(hook.priority());
                lastGroup.add(hook);
                groups.add(lastGroup);
            } else if (lastGroup.priority == hook.priority()) {
                lastGroup.add(hook);
            }
        }
        return groups;
    }

    /**
     * Registers a JVM shutdown hook to trigger the application shutdown process.
     */
    public synchronized void register() {
        if (register.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(shutdownTask);
        }
    }

    /**
     * Unregisters the JVM shutdown hook associated with this {@code Shutdown} instance, preventing
     * the automatic execution of registered shutdown hooks upon JVM shutdown.
     */
    public synchronized void unregister() {
        if (register.compareAndSet(true, false)) {
            Runtime.getRuntime().removeShutdownHook(shutdownTask);
        }
    }

    /**
     * Adds a {@link ShutdownHook} instance to be executed during the shutdown process.
     *
     * @param hook the {@code ShutdownHook} to add
     */
    public void addHook(ShutdownHook hook) {
        if (hook != null) {
            hooks.add(hook);
        }
    }

    /**
     * Adds a {@link Runnable} as a shutdown hook to be executed during the shutdown process.
     *
     * @param runnable the {@code Runnable} to add as a shutdown hook
     */
    public void addHook(Runnable runnable) {
        if (runnable != null) {
            addHook(new ShutdownHookAdapter(runnable));
        }
    }

    /**
     * Checks if the shutdown process has started.
     *
     * @return {@code true} if the shutdown process has started, {@code false} otherwise
     */
    public boolean isShutdown() {
        return shutdown.get();
    }

    /**
     * Initiates the shutdown process and returns a {@link CompletableFuture} representing its completion.
     *
     * @return a {@code CompletableFuture} representing the pending completion of the shutdown process
     */
    public CompletableFuture<Void> shutdown() {
        return doShutdown();
    }

    /**
     * Initiates the shutdown process and waits for its completion within the specified timeout period.
     *
     * @param timeout the timeout in milliseconds to wait for the shutdown process to complete
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @throws ExecutionException   if the shutdown process throws an exception
     * @throws TimeoutException     if the timeout expires before the shutdown process is complete
     */
    public void shutdown(long timeout) throws InterruptedException, ExecutionException, TimeoutException {
        doShutdown().get(timeout, TimeUnit.MILLISECONDS);
    }

}
