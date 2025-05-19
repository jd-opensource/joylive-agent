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
package com.jd.live.agent.plugin.registry.dubbo.v2_6.registry;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.registry.Registry;
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.governance.registry.RegistryService.AbstractSystemRegistryService;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.registry.dubbo.v2_6.instance.DubboEndpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.governance.policy.service.ServiceName.getUniqueName;

/**
 * Registry service implementation for Eureka discovery client.
 * Handles service instance registration and discovery through Eureka server.
 */
public class DubboRegistryService extends AbstractSystemRegistryService implements DubboRegistryPublisher {

    private final Registry registry;

    private final Map<String, URL> urls = new ConcurrentHashMap<>(16);

    public DubboRegistryService(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void subscribe(URL url) {
        String service = url.getServiceInterface();
        String group = url.getParameter(Constants.LABEL_GROUP);
        String key = getUniqueName(null, service, group);
        urls.put(key, url);
    }

    @Override
    protected List<ServiceEndpoint> getEndpoints(String service, String group) throws Exception {
        URL url = urls.get(getUniqueName(null, service, group));
        return url == null ? new ArrayList<>() : toList(registry.lookup(url), DubboEndpoint::new);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DubboRegistryService)) return false;
        return registry == ((DubboRegistryService) o).registry;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(registry);
    }
}
