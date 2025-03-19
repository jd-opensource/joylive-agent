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
package com.jd.live.agent.plugin.router.springcloud.v2_2.registry;

import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceRegistry;
import com.jd.live.agent.plugin.router.springcloud.v2_2.instance.RibbonEndpoint;
import com.netflix.loadbalancer.ILoadBalancer;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;

import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * A {@link ServiceRegistry} implementation for Ribbon-based service discovery and load balancing.
 * This class integrates with Ribbon's {@link ILoadBalancer} to retrieve service endpoints
 * dynamically based on the provided service name and client factory.
 */
public class RibbonServiceRegistry implements ServiceRegistry {

    private final String service;

    private final ILoadBalancer loadBalancer;

    public RibbonServiceRegistry(String service, SpringClientFactory clientFactory) {
        this.service = service;
        this.loadBalancer = clientFactory.getLoadBalancer(service);
    }

    @Override
    public List<ServiceEndpoint> getEndpoints() {
        return toList(loadBalancer.getAllServers(), s -> new RibbonEndpoint(service, s));
    }

    @Override
    public String getService() {
        return service;
    }
}
