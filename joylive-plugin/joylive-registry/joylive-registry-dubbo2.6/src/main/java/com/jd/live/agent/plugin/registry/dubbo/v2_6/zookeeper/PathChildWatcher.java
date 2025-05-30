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

import org.apache.curator.framework.api.CuratorWatcher;

import java.util.List;

/**
 * Watches for changes to the children of a specified ZooKeeper path and triggers callbacks.
 */
public interface PathChildWatcher {

    /**
     * Begins watching the children of the specified path and returns the current children.
     *
     * @param path    the ZooKeeper path to watch for child changes (must exist)
     * @param watcher the callback to invoke when children change (cannot be null)
     * @return the current list of child node names (in natural order), or empty list if no children exist
     */
    List<String> watch(String path, CuratorWatcher watcher) throws Exception;
}
