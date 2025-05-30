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
package com.jd.live.agent.plugin.registry.dubbo.v3.zookeeper;

import org.apache.curator.framework.CuratorFramework;

/**
 * A wrapper for executing {@link CuratorExecution} operations with automatic error handling.
 *
 * @see CuratorExecution
 */
public class CuratorTask {

    private final CuratorFramework client;

    /**
     * Creates a new task instance with the specified Curator client.
     *
     * @param client the Curator framework instance (non-null)
     */
    public CuratorTask(CuratorFramework client) {
        this.client = client;
    }

    /**
     * Executes the operation with automatic error handling.
     *
     * @param pathData  the path and data for the operation (non-null)
     * @param execution the operation to perform (non-null)
     * @throws IllegalStateException for unexpected failures
     */
    public <T> T execute(PathData pathData, CuratorExecution<T> execution) {
        try {
            return execution.execute(pathData, client);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Factory method for fluent task creation.
     *
     * @param client the Curator framework instance (non-null)
     * @return a new task instance
     */
    public static CuratorTask of(CuratorFramework client) {
        return new CuratorTask(client);
    }
}
