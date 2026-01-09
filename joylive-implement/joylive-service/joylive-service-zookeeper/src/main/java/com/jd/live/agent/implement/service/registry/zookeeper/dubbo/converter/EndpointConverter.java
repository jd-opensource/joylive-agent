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
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.converter.BiConverter;
import com.jd.live.agent.governance.registry.ServiceId;
import com.jd.live.agent.implement.service.registry.zookeeper.dubbo.DubboZookeeperEndpoint;
import org.apache.curator.framework.recipes.cache.ChildData;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class EndpointConverter implements BiConverter<ServiceId, ChildData, DubboZookeeperEndpoint> {

    private static final String DUBBO = "dubbo";
    private static final String PROTOCOL = "protocol";
    private static final String GROUP = "group";

    private final ObjectParser parser;

    public EndpointConverter(ObjectParser parser) {
        this.parser = parser;
    }

    @Override
    public DubboZookeeperEndpoint convert(ServiceId serviceId, ChildData data) {
        if (serviceId.isInterfaceMode()) {
            return getInterfaceInstance(serviceId, data);
        } else {
            return getServiceInstance(serviceId, data);
        }
    }

    /**
     * Creates DubboZookeeperEndpoint from Zookeeper service node data.
     *
     * @param serviceId target service ID
     * @param data      Zookeeper node data
     * @return endpoint instance or null if invalid
     */
    private DubboZookeeperEndpoint getServiceInstance(ServiceId serviceId, ChildData data) {
        CuratorInstance<ZookeeperInstance> instance = parser.read(
                new InputStreamReader(new ByteArrayInputStream(data.getData())),
                new TypeReference<CuratorInstance<ZookeeperInstance>>() {
                });
        ZookeeperInstance payload = instance.getPayload();
        Map<String, String> parameters = payload == null ? null : payload.getMetadata();
        String group = parameters == null ? null : parameters.get(GROUP);
        group = group == null || group.isEmpty() ? serviceId.getGroup() : group;
        String params = parameters == null ? null : parameters.get("dubbo.metadata-service.url-params");
        boolean version2 = params != null && params.contains("\"release\":\"2");
        String protocol = version2 ? getProtocol2(params, parser) : getProtocol3(params, parser);
        protocol = protocol == null || protocol.isEmpty() ? DUBBO : protocol;
        return new DubboZookeeperEndpoint(serviceId.getService(), group, protocol, instance.getAddress(), instance.getPort(), parameters);
    }

    /**
     * Extracts the protocol type (version 2 format) from URL parameters.
     *
     * @param params URL parameters string to parse (may be null)
     * @param parser Object parser instance for parameter deserialization
     * @return "dubbo" if Dubbo protocol is found, null otherwise
     */
    private String getProtocol2(String params, ObjectParser parser) {
        try {
            Map<String, Map<String, String>> urlParams = params == null ? null : parser.read(new StringReader(params), new TypeReference<Map<String, Map<String, String>>>() {
            });
            return urlParams == null || !urlParams.containsKey(DUBBO) ? null : DUBBO;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts the protocol type (version 3 format) from URL parameters.
     *
     * @param params URL parameters string to parse (may be null)
     * @param parser Object parser instance for parameter deserialization
     * @return The protocol string if found, null otherwise
     */
    private String getProtocol3(String params, ObjectParser parser) {
        try {
            Map<String, String> urlParams = params == null ? null : parser.read(new StringReader(params), new TypeReference<Map<String, String>>() {
            });
            return urlParams == null ? null : urlParams.get(PROTOCOL);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates DubboZookeeperEndpoint from Zookeeper interface node path.
     *
     * @param serviceId target service ID
     * @param data      Zookeeper node data
     * @return endpoint instance or null if invalid
     */
    private DubboZookeeperEndpoint getInterfaceInstance(ServiceId serviceId, ChildData data) {
        String path = data.getPath();
        int pos = path.lastIndexOf('/');
        path = pos >= 0 ? path.substring(pos + 1) : path;
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (Exception ignored) {
        }
        URI uri = URI.parse(path);
        Map<String, String> parameters = uri.getParameters();
        String group = uri.getParameter(GROUP);
        group = group == null || group.isEmpty() ? serviceId.getGroup() : group;
        return new DubboZookeeperEndpoint(serviceId.getService(), group, uri.getScheme(), uri.getHost(), uri.getPort(), parameters == null ? null : new HashMap<>(parameters));
    }
}
