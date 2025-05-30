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
package com.jd.live.agent.plugin.registry.dubbo.v2_6.zookeeper;

import org.apache.curator.framework.CuratorFramework;

/**
 * Defines callback operations for path-based execution in a ZooKeeper/Curator environment.
 * Provides different handling for successful execution and error cases (node exists/missing).
 */
public interface CuratorExecution<T> {

    /**
     * Executes the primary operation on the specified ZooKeeper node.
     *
     * @param pathData the node path and data to operate on (non-null)
     * @param client   the Curator client instance for ZooKeeper operations (non-null)
     * @throws Exception if execution fails, with specific exceptions triggering fallback behaviors.
     */
    T execute(PathData pathData, CuratorFramework client) throws Exception;

    /**
     * Base implementation of {@link CuratorExecution} for void operations.
     * <p>
     * Provides default null-returning implementations while allowing subclasses
     * to override specific behavior through template methods.
     */
    class CuratorVoidExecution implements CuratorExecution<Void> {

        @Override
        public Void execute(PathData pathData, CuratorFramework client) throws Exception {
            doExecute(pathData, client);
            return null;
        }

        /**
         * Template method for execution logic.
         *
         * @param pathData contains path and associated data
         * @param client   Curator client instance
         * @throws Exception on execution failure
         */
        protected void doExecute(PathData pathData, CuratorFramework client) throws Exception {

        }
    }
}
