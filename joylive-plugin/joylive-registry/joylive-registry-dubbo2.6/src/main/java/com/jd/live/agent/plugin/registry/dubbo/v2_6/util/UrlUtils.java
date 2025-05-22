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

import java.util.HashMap;
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
     * Handles both provider and consumer side URLs with different parsing strategies.
     *
     * @param url the URL to parse
     * @return parsed ServiceId with service name and group
     */
    public static ServiceId toServiceId(URL url) {
        String service = url.getServiceInterface();
        String group = url.getParameter(LABEL_GROUP, "");
        if (group == null || group.isEmpty()) {
            group = url.getParameter(LABEL_SERVICE_GROUP, "");
        }
        return new ServiceId(service, group, true);
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
     * Converts ServiceEndpoint to URL object.
     * Uses default DUBBO scheme if endpoint scheme is null/empty.
     *
     * @param endpoint the service endpoint to convert
     * @return new URL with endpoint data
     */
    public static URL toURL(ServiceEndpoint endpoint) {
        String scheme = endpoint.getScheme();
        scheme = scheme == null || scheme.isEmpty() ? DUBBO : scheme;
        return new URL(scheme, endpoint.getHost(), endpoint.getPort(), endpoint.getService(), endpoint.getMetadata());
    }
}
