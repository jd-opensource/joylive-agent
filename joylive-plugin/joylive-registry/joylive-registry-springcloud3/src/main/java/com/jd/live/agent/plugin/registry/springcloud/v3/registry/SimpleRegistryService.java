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
package com.jd.live.agent.plugin.registry.springcloud.v3.registry;

import com.jd.live.agent.governance.registry.RegistryService;
import com.jd.live.agent.governance.registry.ServiceEndpoint;

import java.util.List;
import java.util.Map;

/**
 * Simple discovery client registry.
 */
public class SimpleRegistryService extends RegistryService.AbstractSystemRegistryService {

    private final Map<String, List<ServiceEndpoint>> endpoints;

    public SimpleRegistryService(Map<String, List<ServiceEndpoint>> endpoints) {
        this.endpoints = endpoints;
    }

    @Override
    protected List<ServiceEndpoint> getEndpoints(String service, String group) {
        return endpoints.get(service);
    }

}
