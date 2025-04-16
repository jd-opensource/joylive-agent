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

import com.alibaba.nacos.client.naming.NacosNamingService;
import com.jd.live.agent.governance.registry.RegistryEvent;
import com.jd.live.agent.governance.registry.RegistryListener;
import com.jd.live.agent.governance.registry.RegistryService.AbstractSystemRegistryService;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.registry.nacos.instance.NacosEndpoint;

import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * Registry service implementation for Eureka discovery client.
 * Handles service instance registration and discovery through Eureka server.
 */
public class NacosRegistryService extends AbstractSystemRegistryService {

    private final NacosNamingService client;

    public NacosRegistryService(NacosNamingService client) {
        this.client = client;
    }

    @Override
    protected List<ServiceEndpoint> getEndpoints(String service, String group) throws Exception {
        return toList(client.getAllInstances(service, group), NacosEndpoint::new);
    }

    /**
     * Publishes an instance event to all subscribed listeners.
     * Only listeners matching the event's service and group will receive it.
     *
     * @param event the instance event to publish (ignored if null)
     */
    public void publish(RegistryEvent event) {
        if (event != null) {
            for (RegistryListener listener : listeners) {
                publish(event, listener);
            }
        }
    }

}
