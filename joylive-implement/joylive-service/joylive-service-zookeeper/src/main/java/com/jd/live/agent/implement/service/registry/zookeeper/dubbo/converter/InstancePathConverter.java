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

import com.jd.live.agent.core.util.converter.BiConverter;
import com.jd.live.agent.core.util.converter.Converter;
import com.jd.live.agent.core.util.converter.TriConverter;
import com.jd.live.agent.governance.registry.ServiceId;
import com.jd.live.agent.governance.registry.ServiceInstance;

import java.util.Map;

public class InstancePathConverter implements BiConverter<ServiceId, ServiceInstance, String> {

    private static final String PROVIDERS = "providers";
    private static final String CONSUMERS = "consumers";
    private static final String SIDE = "side";
    private static final String PROVIDER = "provider";
    private static final String CATEGORY = "category";

    private final Converter<ServiceInstance, String> nodeConverter;
    private final TriConverter<ServiceId, String, String, String> pathConverter;

    public InstancePathConverter(Converter<ServiceInstance, String> nodeConverter,
                                 TriConverter<ServiceId, String, String, String> pathConverter) {
        this.nodeConverter = nodeConverter;
        this.pathConverter = pathConverter;
    }

    @Override
    public String convert(ServiceId serviceId, ServiceInstance instance) {
        if (serviceId.isInterfaceMode()) {
            Map<String, String> metadata = instance.getMetadata();
            String role = instance.getMetadata(CATEGORY, null);
            if (role == null || role.isEmpty()) {
                String side = metadata == null ? null : metadata.get(SIDE);
                side = side == null || side.isEmpty() ? PROVIDER : side;
                role = PROVIDER.equals(side) ? PROVIDERS : CONSUMERS;
            }

            String node = nodeConverter.convert(instance);
            return node == null ? null : pathConverter.convert(serviceId, role, node);
        } else {
            return pathConverter.convert(serviceId, null, instance.getAddress());
        }
    }
}
