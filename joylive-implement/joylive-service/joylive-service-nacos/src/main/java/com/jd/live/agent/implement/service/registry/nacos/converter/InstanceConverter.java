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
package com.jd.live.agent.implement.service.registry.nacos.converter;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.jd.live.agent.core.util.converter.Converter;
import com.jd.live.agent.governance.registry.ServiceInstance;

import java.util.HashMap;

public class InstanceConverter implements Converter<ServiceInstance, Instance> {

    public static final Converter<ServiceInstance, Instance> INSTANCE = new InstanceConverter();

    @Override
    public Instance convert(ServiceInstance source) {
        Instance result = new Instance();
        result.setInstanceId(source.getId());
        result.setIp(source.getHost());
        result.setPort(source.getPort());
        result.setServiceName(source.getService());
        result.setMetadata(source.getMetadata() == null ? null : new HashMap<>(source.getMetadata()));
        result.setWeight(source.getWeight());
        return result;
    }
}
