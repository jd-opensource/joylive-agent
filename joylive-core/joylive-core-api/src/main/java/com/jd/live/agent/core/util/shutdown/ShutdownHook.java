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

import java.util.concurrent.CompletableFuture;

/**
 * Functional interface representing a shutdown hook.
 * <p>
 * A shutdown hook is a thread that is executed when the program is shutting down.
 * This interface defines a method to run during the shutdown process with an optional
 * priority that determines the order of execution if multiple hooks are present.
 * </p>
 */
@FunctionalInterface
public interface ShutdownHook {

    /**
     * The default priority that is used if no other priority is specified.
     */
    int DEFAULT_PRIORITY = 100;

    /**
     * Executes the shutdown hook logic asynchronously.
     * <p>
     * This method should contain the logic to be performed during the shutdown
     * process. It returns a {@link CompletableFuture} which can be used to
     * track the completion of the hook execution.
     * </p>
     *
     * @return a CompletableFuture representing the pending completion of the hook
     */
    CompletableFuture<Void> run();

    /**
     * Returns the priority of this shutdown hook.
     * <p>
     * The priority is used to determine the order in which shutdown hooks are called.
     * A lower number indicates a higher priority. If not overridden, the default
     * priority ({@value #DEFAULT_PRIORITY}) is returned.
     * </p>
     *
     * @return the priority of the hook
     */
    default int priority() {
        return DEFAULT_PRIORITY;
    }
}

