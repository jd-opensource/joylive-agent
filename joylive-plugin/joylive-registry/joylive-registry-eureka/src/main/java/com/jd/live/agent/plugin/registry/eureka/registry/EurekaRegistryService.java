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
package com.jd.live.agent.plugin.registry.eureka.registry;

import com.jd.live.agent.governance.registry.RegistryEvent;
import com.jd.live.agent.governance.registry.RegistryListener;
import com.jd.live.agent.governance.registry.RegistryService.AbstractSystemRegistryService;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.registry.eureka.instance.EurekaEndpoint;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.shared.Application;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * Registry service implementation for Eureka discovery client.
 * Handles service instance registration and discovery through Eureka server.
 */
public class EurekaRegistryService extends AbstractSystemRegistryService implements EurekaRegistryPublisher {

    @Setter
    private DiscoveryClient client;

    public EurekaRegistryService() {
    }

    public EurekaRegistryService(DiscoveryClient client) {
        this.client = client;
    }

    @Override
    protected List<ServiceEndpoint> getEndpoints(String service, String group) {
        return getEndpoints(client.getApplication(service), group);
    }

    @Override
    public void publish(Application application) {
        if (application != null) {
            for (RegistryListener listener : listeners) {
                List<ServiceEndpoint> endpoints = getEndpoints(application, listener.getGroup());
                publish(new RegistryEvent(application.getName(), listener.getGroup(), endpoints, getDefaultGroup()), listener);
            }
        }
    }

    private List<ServiceEndpoint> getEndpoints(Application application, String group) {
        return application == null ? null : toList(application.getInstances(), i -> {
            String groupName = i.getAppGroupName();
            if (group != null && !group.isEmpty()) {
                return group.equals(groupName) ? new EurekaEndpoint(i) : null;
            }
            return groupName == null || groupName.isEmpty() ? new EurekaEndpoint(i) : null;
        });
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EurekaRegistryService)) return false;
        return client == ((EurekaRegistryService) o).client;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(client);
    }
}
