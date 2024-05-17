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
 * An adapter class for {@link ShutdownHook} that allows the wrapping of {@link Runnable} instances
 * or other {@code ShutdownHook} instances with custom priority and execution mode settings.
 */
public class ShutdownHookAdapter implements ShutdownHook {

    /**
     * The wrapped {@link ShutdownHook} instance.
     */
    protected ShutdownHook hook;

    /**
     * The priority of the shutdown hook.
     */
    protected int priority;

    /**
     * Constructs a {@code ShutdownHookAdapter} with a {@link Runnable} to be executed
     * at the default priority and in a blocking manner.
     *
     * @param runnable the runnable to be executed during shutdown
     */
    public ShutdownHookAdapter(Runnable runnable) {
        this(runnable, DEFAULT_PRIORITY, false);
    }

    /**
     * Constructs a {@code ShutdownHookAdapter} with a {@link Runnable} to be executed
     * at the specified priority and in a blocking manner.
     *
     * @param runnable the runnable to be executed during shutdown
     * @param priority the priority of the shutdown hook
     */
    public ShutdownHookAdapter(Runnable runnable, int priority) {
        this(runnable, priority, false);
    }

    /**
     * Constructs a {@code ShutdownHookAdapter} with a {@link Runnable} to be executed
     * at the specified priority, with an option for asynchronous execution.
     *
     * @param runnable the runnable to be executed during shutdown
     * @param priority the priority of the shutdown hook
     * @param async    if {@code true}, the runnable will be executed asynchronously
     */
    public ShutdownHookAdapter(Runnable runnable, int priority, boolean async) {
        this.hook = !async ? () -> {
            runnable.run();
            return CompletableFuture.completedFuture(null);
        } : () -> CompletableFuture.runAsync(runnable);
        this.priority = priority;
    }

    /**
     * Executes the wrapped shutdown logic.
     *
     * @return a CompletableFuture representing the pending completion of the hook
     */
    @Override
    public CompletableFuture<Void> run() {
        return hook.run();
    }

    /**
     * Returns the priority of this shutdown hook.
     *
     * @return the priority of the hook
     */
    @Override
    public int priority() {
        return priority;
    }
}

