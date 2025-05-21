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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.util;

import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceId;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.jd.live.agent.governance.util.FrameworkVersion;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.instance.DubboEndpoint;
import org.apache.dubbo.common.URL;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.Constants.*;
import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.RegistryConstants.*;

/**
 * Utility class for URL parsing operations.
 */
public class UrlUtils {

    private static final String DUBBO = "dubbo";
    private static final String RELEASE = "release";
    private static final String VERSION = "2.7";

    /**
     * Parses a URL into a ServiceId containing service name and group.
     *
     * @param url the URL to parse
     * @return parsed ServiceId with service name and group
     */
    public static ServiceId toServiceId(URL url) {
        String service;
        String side = url.getParameter(SIDE_KEY, PROVIDER_SIDE);
        if (CONSUMER_SIDE.equalsIgnoreCase(side)) {
            service = url.getParameter(PROVIDED_BY);
            if (service != null && !service.isEmpty()) {
                // group is not valid for service.
                // group is valid for interface.
                return new ServiceId(service, "", false);
            }
        } else if (isServiceMode(url)) {
            return new ServiceId(url.getParameter(PROVIDED_BY), "", false);
        }
        service = url.getServiceInterface();
        String group = url.getParameter(LABEL_GROUP, "");
        if (group == null || group.isEmpty()) {
            group = url.getParameter(LABEL_SERVICE_GROUP, "");
        }
        return new ServiceId(service, group, true);
    }

    /**
     * Checks if URL represents service registration mode.
     *
     * @param url Dubbo URL to check
     * @return true if service mode
     */
    public static boolean isServiceMode(URL url) {
        return SERVICE_REGISTRY_TYPE.equalsIgnoreCase(url.getParameter(REGISTRY_TYPE_KEY));
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
        metadata.remove(REGISTRY_TYPE_KEY);
        ServiceId serviceId = toServiceId(url);
        return ServiceInstance.builder()
                .interfaceMode(serviceId.isInterfaceMode())
                .framework(new FrameworkVersion(DUBBO, url.getParameter(RELEASE, VERSION)))
                .service(serviceId.getService())
                .group(serviceId.getGroup())
                .scheme(url.getProtocol())
                .host(url.getHost())
                .port(url.getPort())
                .weight(url.getParameter(LABEL_WEIGHT, 100))
                .metadata(metadata)
                .build();
    }

    /**
     * Converts a Dubbo registry ServiceInstance to standard ServiceInstance representation.
     * <p>
     * Handles metadata processing including:
     *
     * @param instance    the Dubbo registry instance to convert
     * @param application the application context for label registration
     * @param parser      the object parser for metadata parameter processing
     * @return ServiceInstance built from registry instance
     * @throws RuntimeException if metadata parameter parsing fails
     */
    public static ServiceInstance toInstance(org.apache.dubbo.registry.client.ServiceInstance instance,
                                             Application application,
                                             ObjectParser parser) {
        Map<String, String> metadata = instance.getMetadata();
        application.labelRegistry(metadata::putIfAbsent);
        MapOption option = new MapOption(metadata);
        String params = option.getString("dubbo.metadata-service.url-params");
        Map<String, Map<String, String>> urlParams = params == null ? null : parser.read(new StringReader(params), new TypeReference<Map<String, Map<String, String>>>() {
        });
        Map<String, String> dubboParams = urlParams == null ? null : urlParams.get(DUBBO);
        MapOption urlOption = new MapOption(dubboParams);
        return ServiceInstance.builder()
                .id(instance.getId())
                .interfaceMode(false)
                .framework(new FrameworkVersion(DUBBO, urlOption.getString(RELEASE, VERSION)))
                .service(instance.getServiceName())
                .scheme(urlOption.getString("protocol", DUBBO))
                .group(option.getString(LABEL_GROUP))
                .host(instance.getHost())
                .port(instance.getPort())
                .weight(option.getInteger(LABEL_WEIGHT, 100))
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
