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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.zookeeper;

import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;

/**
 * Composite listener interface combining {@link CuratorWatcher} and {@link TreeCacheListener} capabilities.
 * <p>
 * Implementations can receive both single-path watcher events and recursive tree cache events
 * through a single interface.
 *
 * @see CuratorWatcher For single-path watch functionality
 * @see TreeCacheListener For recursive tree monitoring
 */
public interface CuratorListener extends CuratorWatcher, TreeCacheListener {

    void removeDataListener();

    void removeChildrenListener();
}
