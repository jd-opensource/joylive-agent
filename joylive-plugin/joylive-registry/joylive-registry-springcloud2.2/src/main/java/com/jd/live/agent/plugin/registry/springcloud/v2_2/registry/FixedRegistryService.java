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
package com.jd.live.agent.plugin.registry.springcloud.v2_2.registry;

import com.jd.live.agent.governance.registry.RegistryService.AbstractSystemRegistryService;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceId;

import java.util.List;
import java.util.Objects;

/**
 * Fixed instance supplier registry service.
 */
public class FixedRegistryService extends AbstractSystemRegistryService {

    private final String service;

    private final List<ServiceEndpoint> endpoints;

    public FixedRegistryService(String service, List<ServiceEndpoint> endpoints) {
        super("fixed://");
        this.service = service;
        this.endpoints = endpoints;
    }

    @Override
    protected List<ServiceEndpoint> getEndpoints(ServiceId serviceId) throws Exception {
        return Objects.equals(service, serviceId.getService()) ? endpoints : null;
    }
}
