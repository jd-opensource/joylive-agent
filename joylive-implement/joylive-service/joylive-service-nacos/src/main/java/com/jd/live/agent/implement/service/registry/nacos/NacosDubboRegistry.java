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

import com.jd.live.agent.governance.config.RegistryClusterConfig;
import com.jd.live.agent.governance.registry.ServiceInstance;

import static com.jd.live.agent.core.util.StringUtils.join;

public class NacosDubboRegistry extends NacosRegistry {

    private static final String SEPARATOR = System.getProperty("nacos.service.name.separator", ":");
    private static final char SEPARATOR_CHAR = SEPARATOR.isEmpty() ? ':' : SEPARATOR.charAt(0);

    public NacosDubboRegistry(RegistryClusterConfig config) {
        super(config);
    }

    @Override
    protected String getService(String service, ServiceInstance instance) {
        String category = instance.getMetadata("category", "providers");
        String[] parts = new String[]{category, service, instance.getVersion(), instance.getGroup()};
        return join(parts, SEPARATOR_CHAR);
    }
}
