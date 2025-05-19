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
package com.jd.live.agent.plugin.registry.nacos.registry;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.jd.live.agent.governance.registry.RegistryEvent;
import com.jd.live.agent.governance.registry.ServiceEndpoint;

import java.util.List;

/**
 * Publisher for Nacos registry events.
 * <p>
 * Allows publishing {@link RegistryEvent} to subscribed listeners and
 * converting Nacos {@link Instance} objects to service endpoints.
 * <p>
 */
public interface NacosRegistryPublisher {

    /**
     * Thread-local storage for current publisher instance
     */
    ThreadLocal<NacosInstancePublisher> LOCAL_PUBLISHER = new ThreadLocal<>();

    /**
     * Publishes an instance event to all subscribed listeners.
     * Only listeners matching the event's service and group will receive it.
     *
     * @param event the instance event to publish (ignored if null)
     */
    void publish(RegistryEvent event);

    /**
     * Converts Nacos instances to service endpoints
     *
     * @param instances List of Nacos service instances
     * @return Converted endpoints (empty list if input is null/empty)
     */
    List<ServiceEndpoint> convert(List<Instance> instances);
}
