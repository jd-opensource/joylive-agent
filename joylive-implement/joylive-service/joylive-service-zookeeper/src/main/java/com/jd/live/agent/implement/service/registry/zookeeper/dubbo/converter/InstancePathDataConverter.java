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

import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.converter.BiConverter;
import com.jd.live.agent.governance.registry.ServiceId;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.jd.live.agent.implement.service.registry.zookeeper.PathData;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class InstancePathDataConverter implements BiConverter<ServiceId, ServiceInstance, PathData> {

    private final ObjectParser parser;

    private final BiConverter<ServiceId, ServiceInstance, String> instancePathConverter;

    public InstancePathDataConverter(ObjectParser parser,
                                     BiConverter<ServiceId, ServiceInstance, String> instancePathConverter) {
        this.parser = parser;
        this.instancePathConverter = instancePathConverter;
    }

    @Override
    public PathData convert(ServiceId serviceId, ServiceInstance instance) {
        String path = instancePathConverter.convert(serviceId, instance);
        byte[] data = new byte[0];
        if (!instance.isInterfaceMode()) {
            String address = instance.getAddress();
            ZookeeperInstance zi = new ZookeeperInstance(address, serviceId.getService(), instance.getMetadata());
            CuratorInstance<ZookeeperInstance> ci = new CuratorInstance<>(address, serviceId.getService(), instance.getHost(), instance.getPort(), zi);
            StringWriter writer = new StringWriter(2048);
            parser.write(writer, ci);
            data = writer.toString().getBytes(StandardCharsets.UTF_8);
        }
        return path == null ? null : new PathData(path, data);
    }

}
