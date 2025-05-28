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

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.converter.Converter;
import com.jd.live.agent.governance.registry.ServiceInstance;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.TreeMap;

/**
 * Converts a ServiceInstance to its URL-encoded string representation.
 */
public class NodeConverter implements Converter<ServiceInstance, String> {

    @Override
    public String convert(ServiceInstance instance) {
        URI uri = URI.builder()
                .schema(instance.getScheme())
                .host(instance.getHost())
                .port(instance.getPort())
                .path(instance.getService())
                .parameters(instance.getMetadata() == null ? null : new TreeMap<>(instance.getMetadata()))
                .build();
        try {
            return URLEncoder.encode(uri.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
