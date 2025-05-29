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
package com.jd.live.agent.plugin.registry.nacos.v1_4.registry;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.naming.NacosNamingService;
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.governance.registry.RegistryService.AbstractSystemRegistryService;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceId;
import com.jd.live.agent.plugin.registry.nacos.v1_4.instance.NacosEndpoint;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.governance.registry.ServiceInstance.getSchemeAddress;

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
        super(getSchemeAddress("nacos", properties.getProperty(SERVER_ADDR), null));
        this.client = client;
        this.secure = Converts.getBoolean(properties.getProperty(Constants.LABEL_SECURE), false);
        this.group = properties.getProperty(Constants.LABEL_GROUP);
        this.namespace = properties.getProperty(Constants.LABEL_NAMESPACE);
    }

    @Override
    protected List<ServiceEndpoint> getEndpoints(ServiceId serviceId) throws Exception {
        return client == null ? new ArrayList<>() : convert(client.getAllInstances(serviceId.getService(), serviceId.getGroup()));
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
