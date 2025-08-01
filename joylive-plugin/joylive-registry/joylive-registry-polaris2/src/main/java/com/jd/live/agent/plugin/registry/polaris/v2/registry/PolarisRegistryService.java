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
package com.jd.live.agent.plugin.registry.polaris.v2.registry;

import com.jd.live.agent.governance.registry.RegistryService.AbstractSystemRegistryService;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceId;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.jd.live.agent.plugin.registry.polaris.v2.instance.PolarisEndpoint;
import com.tencent.polaris.api.plugin.registry.LocalRegistry;
import com.tencent.polaris.api.plugin.registry.ResourceFilter;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;

import java.util.ArrayList;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * Registry service implementation for Polaris in memory registry.
 * Handles service instance registration and discovery through Eureka server.
 */
public class PolarisRegistryService extends AbstractSystemRegistryService {

    private final LocalRegistry registry;

    public PolarisRegistryService(LocalRegistry registry) {
        super("polaris");
        this.registry = registry;
    }

    @Override
    protected List<ServiceEndpoint> getEndpoints(ServiceId serviceId) {
        ServiceKey serviceKey = new ServiceKey(serviceId.getNamespace(), serviceId.getService());
        ServiceEventKey eventKey = new ServiceEventKey(serviceKey, ServiceEventKey.EventType.INSTANCE);
        ServiceInstances instances = registry.getInstances(new ResourceFilter(eventKey, true, true));
        return instances.isInitialized() ? new ArrayList<>() : toList(instances.getInstances(), PolarisEndpoint::new);
    }

    @Override
    public void unregister(ServiceId serviceId, ServiceInstance instance) {
        // shutdown
    }
}
