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

import com.jd.live.agent.core.util.converter.BiConverter;
import com.jd.live.agent.governance.registry.RegistryDeltaEvent;
import com.jd.live.agent.governance.registry.RegistryEvent;
import com.jd.live.agent.governance.registry.ServiceId;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

import java.util.function.Consumer;

import static com.jd.live.agent.core.util.CollectionUtils.singletonList;

/**
 * Listener for ZooKeeper path children changes, converting them to registry events.
 *
 * <p>Handles CHILD_ADDED/REMOVED/UPDATED events and notifies consumer when:
 * 1. The endpoint is valid
 * 2. The group matches the service group
 */
public class DubboChildrenCacheListener implements PathChildrenCacheListener {
    private final ServiceId serviceId;
    private final Consumer<RegistryEvent> consumer;
    private final BiConverter<ServiceId, ChildData, DubboZookeeperEndpoint> converter;

    public DubboChildrenCacheListener(ServiceId serviceId,
                                      Consumer<RegistryEvent> consumer,
                                      BiConverter<ServiceId, ChildData, DubboZookeeperEndpoint> converter) {
        this.serviceId = serviceId;
        this.consumer = consumer;
        this.converter = converter;
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        RegistryDeltaEvent.EventType eventType = null;
        switch (event.getType()) {
            case CHILD_ADDED:
                eventType = RegistryDeltaEvent.EventType.ADD;
                break;
            case CHILD_REMOVED:
                eventType = RegistryDeltaEvent.EventType.REMOVE;
                break;
            case CHILD_UPDATED:
                eventType = RegistryDeltaEvent.EventType.UPDATE;
                break;
        }
        if (eventType != null) {
            DubboZookeeperEndpoint endpoint = converter.convert(serviceId, event.getData());
            if (endpoint != null && match(serviceId.getGroup(), endpoint.getGroup())) {
                consumer.accept(new RegistryDeltaEvent(serviceId, singletonList(endpoint), eventType));
            }
        }
    }

    /**
     * Checks if two service groups match (null/empty considered equal).
     *
     * @return true if groups are equivalent
     */
    private boolean match(String group1, String group2) {
        if (group1 == null || group1.isEmpty()) {
            return group2 == null || group2.isEmpty();
        }
        return group1.equals(group2);
    }
}
