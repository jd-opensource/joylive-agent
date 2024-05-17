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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A group of {@link ShutdownHook} instances that can be executed together as a single shutdown hook.
 * <p>
 * This class allows multiple shutdown hooks to be registered and managed as a group, with the
 * group itself being treated as a single shutdown hook. It facilitates executing multiple shutdown
 * tasks that share the same priority level.
 * </p>
 */
public class ShutdownHookGroup implements ShutdownHook {

    /**
     * A list of {@code ShutdownHook} instances that belong to this group.
     */
    protected List<ShutdownHook> hooks = new ArrayList<>();

    /**
     * The priority of the shutdown hook group.
     */
    protected int priority;

    /**
     * Constructs a {@code ShutdownHookGroup} with the specified priority.
     *
     * @param priority the priority of the shutdown hook group
     */
    public ShutdownHookGroup(int priority) {
        this.priority = priority;
    }

    public int size() {
        return hooks.size();
    }

    /**
     * Adds a shutdown hook to the group.
     *
     * @param hook the {@code ShutdownHook} to add to the group
     */
    public void add(final ShutdownHook hook) {
        if (hook != null) {
            hooks.add(hook);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Void> run() {
        switch (hooks.size()) {
            case 0:
                return CompletableFuture.completedFuture(null);
            case 1:
                return hooks.get(0).run();
            default:
                CompletableFuture<Void>[] futures = new CompletableFuture[hooks.size()];
                int i = 0;
                for (ShutdownHook hook : hooks) {
                    futures[i++] = hook.run();
                }
                return CompletableFuture.allOf(futures);
        }
    }

    @Override
    public int priority() {
        return priority;
    }
}
