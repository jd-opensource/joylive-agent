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
package com.jd.live.agent.implement.service.registry.nacos;

import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.RegistryClusterConfig;
import com.jd.live.agent.governance.probe.HealthProbe;
import com.jd.live.agent.governance.registry.ServiceId;
import com.jd.live.agent.governance.registry.ServiceInstance;

public class NacosDubboRegistry extends NacosRegistry {

    private static final String SEPARATOR = System.getProperty("nacos.service.name.separator", ":");

    public NacosDubboRegistry(RegistryClusterConfig config, HealthProbe probe, Timer timer) {
        super(config, probe, timer);
    }

    @Override
    protected String getService(ServiceId serviceId, ServiceInstance instance) {
        if (!serviceId.isInterfaceMode()) {
            return serviceId.getService();
        }
        String category = instance.getMetadata("category", "providers");
        StringBuilder builder = new StringBuilder(56)
                .append(category).append(SEPARATOR)
                .append(serviceId.getService()).append(':')
                .append(instance.getVersion() == null ? "" : instance.getVersion()).append(':')
                .append(serviceId.getGroup() == null ? "" : serviceId.getGroup());
        return builder.toString();
    }
}
