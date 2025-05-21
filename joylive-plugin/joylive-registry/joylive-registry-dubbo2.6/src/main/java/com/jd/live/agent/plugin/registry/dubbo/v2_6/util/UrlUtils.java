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
package com.jd.live.agent.plugin.registry.dubbo.v2_6.util;

import com.alibaba.dubbo.common.URL;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceId;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.jd.live.agent.governance.util.FrameworkVersion;
import com.jd.live.agent.plugin.registry.dubbo.v2_6.instance.DubboEndpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.Constants.*;

/**
 * Utility class for URL parsing operations.
 */
public class UrlUtils {

    private static final String DUBBO = "dubbo";
    private static final String RELEASE = "release";
    private static final String VERSION = "2.6";

    /**
     * Parses a URL into a ServiceId containing service name and group.
     *
     * @param url the URL to parse
     * @return parsed ServiceId with service name and group
     */
    public static ServiceId parse(URL url) {
        String serviceName = url.getServiceInterface();
        String group = url.getParameter(LABEL_GROUP, "");
        if (group == null || group.isEmpty()) {
            group = url.getParameter(LABEL_SERVICE_GROUP, "");
        }
        return new ServiceId(serviceName, group);
    }

    /**
     * Converts a Dubbo URL to a ServiceInstance representation.
     * <p>
     * Creates an interface-mode instance with metadata derived from URL parameters.
     *
     * @param url the Dubbo URL to convert
     * @return ServiceInstance built from URL parameters
     */
    public static ServiceInstance toInstance(URL url) {
        Map<String, String> metadata = new HashMap<>(url.getParameters());
        // application.labelRegistry(metadata::putIfAbsent);
        return ServiceInstance.builder()
                .interfaceMode(true)
                .framework(new FrameworkVersion(DUBBO, url.getParameter(RELEASE, VERSION)))
                .service(url.getServiceInterface())
                .group(url.getParameter(LABEL_GROUP))
                .scheme(url.getProtocol())
                .host(url.getHost())
                .port(url.getPort())
                .weight(url.getParameter(LABEL_WEIGHT, 100))
                .metadata(metadata)
                .build();
    }

    /**
     * Groups service instances by their group name.
     *
     * @param urls list of service instances to group
     * @return map of group names to their corresponding endpoints
     */
    public static Map<String, List<ServiceEndpoint>> toInstance(List<URL> urls) {
        Map<String, List<ServiceEndpoint>> endpoints = new HashMap<>(4);
        String group;
        List<ServiceEndpoint> lasts = null;
        ServiceEndpoint current;
        ServiceEndpoint last = null;
        for (URL url : urls) {
            current = new DubboEndpoint(url);
            group = current.getGroup();
            if (last == null || !current.getGroup().equals(last.getGroup())) {
                lasts = endpoints.computeIfAbsent(group, k -> new ArrayList<>());
            }
            last = current;
            lasts.add(current);
        }
        return endpoints;
    }
}
