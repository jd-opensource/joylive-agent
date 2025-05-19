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
package com.jd.live.agent.plugin.registry.nacos.v2_4.registry;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.naming.NacosNamingService;
import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.governance.registry.RegistryService.AbstractSystemRegistryService;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.registry.nacos.v2_4.instance.NacosEndpoint;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * Registry service implementation for Eureka discovery client.
 * Handles service instance registration and discovery through Eureka server.
 */
public class NacosRegistryService extends AbstractSystemRegistryService implements NacosRegistryPublisher {

    @Setter
    private NacosNamingService client;

    private final Boolean secure;

    private final String group;

    private final String namespace;

    public NacosRegistryService(Properties properties) {
        this(null, properties);
    }

    public NacosRegistryService(NacosNamingService client, Properties properties) {
        this.client = client;
        this.secure = properties != null && Converts.getBoolean(properties.getProperty(NacosEndpoint.KEY_SECURE), false);
        this.group = properties == null ? null : properties.getProperty(NacosEndpoint.KEY_GROUP);
        this.namespace = properties == null ? null : properties.getProperty(NacosEndpoint.KEY_NAMESPACE);
    }

    @Override
    protected List<ServiceEndpoint> getEndpoints(String service, String group) throws Exception {
        return client == null ? new ArrayList<>() : convert(client.getAllInstances(service, group));
    }

    @Override
    public List<ServiceEndpoint> convert(List<Instance> instances) {
        return toList(instances, e -> new NacosEndpoint(e, secure));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NacosRegistryService)) return false;
        return client == ((NacosRegistryService) o).client;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(client);
    }
}
