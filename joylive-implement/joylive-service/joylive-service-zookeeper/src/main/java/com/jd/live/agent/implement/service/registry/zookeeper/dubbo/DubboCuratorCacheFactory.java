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
package com.jd.live.agent.implement.service.registry.zookeeper.dubbo;

import com.jd.live.agent.governance.registry.RegistryEvent;
import com.jd.live.agent.governance.registry.ServiceId;
import org.apache.curator.framework.recipes.cache.CuratorCache;

import java.util.function.Consumer;

/**
 * Factory for creating CuratorCache instances with Dubbo service awareness.
 */
public interface DubboCuratorCacheFactory {

    /**
     * Creates a configured CuratorCache for service discovery.
     *
     * @param serviceId the Dubbo service identifier
     * @param path      the ZooKeeper node path to watch
     * @param consumer  the event handler for registry changes
     * @return a configured CuratorCache instance
     */
    CuratorCache create(ServiceId serviceId, String path, Consumer<RegistryEvent> consumer);
}

