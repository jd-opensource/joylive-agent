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

import com.jd.live.agent.core.extension.annotation.Extensible;

import java.util.concurrent.CompletableFuture;

/**
 * Defines an interface for an agent service that supports asynchronous start and stop operations.
 * This interface allows for non-blocking execution and easy chaining or combination with other asynchronous operations.
 * The asynchronous nature is facilitated by the use of {@link CompletableFuture} for both starting and stopping the service,
 * providing a way to handle completion or failure without blocking the current thread.
 *
 * @since 1.0.0
 */
@Extensible("AgentService")
public interface AgentService {

    /**
     * Asynchronously starts the agent service.
     * This method returns a {@link CompletableFuture} that completes when the service has successfully started.
     * It allows for further action to be taken in a non-blocking manner, such as chaining further asynchronous operations
     * or handling service start completion or failure.
     *
     * @return A {@link CompletableFuture} that is completed when the service has started.
     */
    CompletableFuture<Void> start();

    /**
     * Asynchronously stops the agent service.
     * This method returns a {@link CompletableFuture} that completes when the service has successfully stopped.
     * Similar to the start method, it enables handling the stop operation in a non-blocking way, allowing the application
     * to perform cleanup, release resources, or trigger other actions upon service stop completion or failure.
     *
     * @return A {@link CompletableFuture} that is completed when the service has stopped.
     */
    CompletableFuture<Void> stop();
}

