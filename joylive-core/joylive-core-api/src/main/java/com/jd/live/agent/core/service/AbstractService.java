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
package com.jd.live.agent.core.service;

import com.jd.live.agent.core.exception.StatusException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides a base implementation for services that adhere to the {@link AgentService} interface,
 * offering a framework for managing service lifecycle states (start and stop) asynchronously.
 * This abstract class introduces a standard way to track and update service status while providing
 * hooks for the actual start and stop operations that must be implemented by subclasses.
 *
 * @since 1.0.0
 */
public abstract class AbstractService implements AgentService {

    /**
     * Service status indicating the service has not been started yet.
     */
    protected static final int STATUS_INITIAL = 0;

    /**
     * Service status indicating the service is in the process of starting.
     */
    protected static final int STATUS_STARTING = 1;

    /**
     * Service status indicating the service has successfully started and is currently running.
     */
    protected static final int STATUS_STARTED = 2;

    /**
     * Service status indicating the service is in the process of stopping.
     */
    protected static final int STATUS_STOPPING = 3;

    /**
     * Current status of the service, managed atomically to ensure thread safety.
     */
    protected final AtomicInteger status = new AtomicInteger(STATUS_INITIAL);

    /**
     * Future representing the ongoing start operation, if any.
     */
    protected CompletableFuture<Void> startFuture;

    /**
     * Future representing the ongoing stop operation, if any.
     */
    protected CompletableFuture<Void> stopFuture;

    protected String name;

    @Override
    public CompletableFuture<Void> start() {
        int current = status.get();
        if (status.compareAndSet(STATUS_INITIAL, STATUS_STARTING)) {
            final CompletableFuture<Void> result = new CompletableFuture<>();
            stopFuture = null;
            startFuture = result;
            // Initiates the start sequence
            doStart().whenComplete((v, t) -> {
                if (t == null) {
                    status.set(STATUS_STARTED);
                    result.complete(null);
                } else {
                    // In case of start failure, attempts to stop to clean up
                    doStop().whenComplete((r, e) -> {
                        status.set(STATUS_INITIAL);
                        result.completeExceptionally(t);
                    });
                }
            });
            return result;
        } else if (current == STATUS_STOPPING) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            result.completeExceptionally(new StatusException("Service is stopping."));
            return result;
        } else {
            // If already starting or started, returns the existing future
            return startFuture;
        }
    }

    @Override
    public CompletableFuture<Void> stop() {
        int current = status.get();
        if (status.compareAndSet(STATUS_STARTED, STATUS_STOPPING)) {
            final CompletableFuture<Void> result = new CompletableFuture<>();
            stopFuture = result;
            startFuture = null;
            // Initiates the stop sequence
            doStop().whenComplete((v, t) -> {
                status.set(STATUS_INITIAL);
                if (t == null) {
                    result.complete(null);
                } else {
                    result.completeExceptionally(t);
                }
            });
            return result;
        } else if (current == STATUS_STARTING) {
            final CompletableFuture<Void> result = new CompletableFuture<>();
            // If currently starting, waits for start to complete before stopping
            startFuture.whenComplete((v, t) -> stop().whenComplete((r, e) -> {
                if (e == null) {
                    result.complete(null);
                } else {
                    result.completeExceptionally(e);
                }
            }));
            return result;
        } else if (current == STATUS_INITIAL) {
            // If not started, completes immediately
            return CompletableFuture.completedFuture(null);
        } else {
            // If already stopping, returns the existing future
            return stopFuture;
        }
    }

    /**
     * Checks if the service is started or in the process of starting.
     *
     * @return {@code true} if the service is started or starting, {@code false} otherwise.
     */
    public boolean isStarted() {
        int state = status.get();
        return state == STATUS_STARTED || state == STATUS_STARTING;
    }

    /**
     * Initiates the start sequence of the service. This method should be implemented by subclasses
     * to define the specific start logic.
     *
     * @return A {@link CompletableFuture} that is completed when the start operation is finished.
     */
    protected abstract CompletableFuture<Void> doStart();

    /**
     * Initiates the stop sequence of the service. This method should be implemented by subclasses
     * to define the specific stop logic.
     *
     * @return A {@link CompletableFuture} that is completed when the stop operation is finished.
     */
    protected abstract CompletableFuture<Void> doStop();

    /**
     * Retrieves the name of the service. This method should be implemented by subclasses
     * to provide a unique identifier for the service.
     *
     * @return A {@link String} representing the name of the service.
     */
    public String getName() {
        if (name == null) {
            String className = this.getClass().getSimpleName();
            StringBuilder builder = new StringBuilder();
            int index = 0;
            for (char c : className.toCharArray()) {
                if (Character.isUpperCase(c)) {
                    if (index > 0) {
                        builder.append("-");
                    }
                    builder.append(Character.toLowerCase(c));
                } else {
                    builder.append(c);
                }
                index++;
            }
            name = builder.toString();
        }
        return name;
    }
}

