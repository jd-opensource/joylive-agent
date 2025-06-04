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
package com.jd.live.agent.implement.service.registry.zookeeper.dubbo.converter;

import com.jd.live.agent.core.util.converter.TriConverter;
import com.jd.live.agent.governance.registry.ServiceId;

import static com.jd.live.agent.core.util.StringUtils.url;

/**
 * Converts service components into formatted path strings based on service mode.
 */
public class ServicePathConverter implements TriConverter<ServiceId, String, String, String> {

    private final String interfaceRoot;

    private final String serviceRoot;

    public ServicePathConverter(String interfaceRoot, String serviceRoot) {
        this.interfaceRoot = interfaceRoot;
        this.serviceRoot = serviceRoot;
    }

    @Override
    public String convert(ServiceId serviceId, String role, String node) {
        if (!serviceId.isInterfaceMode()) {
            return url(serviceRoot, serviceId.getService(), node);
        }
        return url(url(interfaceRoot, serviceId.getService()), role, node);
    }
}
