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
import lombok.Getter;
import org.apache.dubbo.common.URL;

import java.io.StringReader;
import java.util.HashMap;
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
    private static final String PROTOCOL = "protocol";

    public static String getSchemeAddress(URL url) {
        return ServiceInstance.getSchemeAddress(url.getProtocol(), url.getHost(), url.getPort());
    }

    /**
     * Parses a URL into a ServiceId containing service name and group.
     * Handles both provider and consumer side URLs with different parsing strategies.
     *
     * @param url the URL to parse
     * @return parsed ServiceId with service name and group
     */
    public static ServiceId toServiceId(URL url) {
        return toServiceId(url, false);
    }

    /**
     * Parses a URL into a ServiceId with configurable parsing mode.
     * Supports three parsing modes:
     * 1. Consumer side with service reference (provided.by)
     * 2. Provider side in service mode (application name)
     * 3. Default interface mode (service interface)
     *
     * @param url                the URL to parse
     * @param forceInterfaceMode if true, forces interface mode parsing
     * @return parsed ServiceId containing service name, group and interface flag
     */
    public static ServiceId toServiceId(URL url, boolean forceInterfaceMode) {
        String service;
        boolean interfaceMode = forceInterfaceMode;
        String side = url.getParameter(SIDE_KEY, PROVIDER_SIDE);
        if (CONSUMER_SIDE.equalsIgnoreCase(side)) {
            if (forceInterfaceMode) {
                service = url.getServiceInterface();
            } else {
                service = url.getParameter(PROVIDED_BY);
                if (service == null || service.isEmpty()) {
                    interfaceMode = true;
                    service = url.getServiceInterface();
                }
            }
        } else if (!forceInterfaceMode && isServiceMode(url)) {
            service = url.getParameter(PROVIDED_BY);
        } else {
            service = url.getServiceInterface();
            interfaceMode = true;
        }
        String group = url.getParameter(LABEL_GROUP, "");
        if (group == null || group.isEmpty()) {
            group = url.getParameter(LABEL_SERVICE_GROUP, "");
        }
        return new ServiceId(service, group, interfaceMode);
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
        return toInstance(url, false);
    }

    /**
     * Converts a Dubbo URL to a ServiceInstance representation.
     * <p>
     * Creates an interface-mode instance with metadata derived from URL parameters.
     *
     * @param url                the Dubbo URL to convert
     * @param forceInterfaceMode if true, forces interface mode parsing
     * @return ServiceInstance built from URL parameters
     */
    public static ServiceInstance toInstance(URL url, boolean forceInterfaceMode) {
        Map<String, String> metadata = new HashMap<>(url.getParameters());
        ServiceId serviceId = toServiceId(url, forceInterfaceMode);
        metadata.remove(REGISTRY_TYPE_KEY);
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
        application.labelRegistry(metadata::put);
        MapOption option = new MapOption(metadata);
        URLParams urlParams = getUrlParams(metadata, parser);
        return ServiceInstance.builder()
                .interfaceMode(false)
                .framework(new FrameworkVersion(DUBBO, urlParams.getRelease()))
                .service(instance.getServiceName())
                .scheme(urlParams.getProtocol())
                .group(instance.getMetadata(LABEL_GROUP))
                .host(instance.getHost())
                .port(instance.getPort())
                .weight(option.getInteger(LABEL_WEIGHT, 100))
                .metadata(metadata)
                .build();
    }

    /**
     * Extracts URL parameters from metadata map.
     *
     * @param metadata source metadata map (can be null)
     * @param parser   parser for converting string to map
     * @return URLParams containing protocol and version (defaults to DUBBO/VERSION if not found)
     */
    public static URLParams getUrlParams(Map<String, String> metadata, ObjectParser parser) {
        String params = metadata == null ? null : metadata.get("dubbo.metadata-service.url-params");
        Map<String, String> urlParams = params == null ? null : parser.read(new StringReader(params), new TypeReference<Map<String, String>>() {
        });
        String protocol = urlParams == null ? DUBBO : urlParams.getOrDefault(PROTOCOL, DUBBO);
        String release = urlParams == null ? VERSION : urlParams.getOrDefault(RELEASE, VERSION);
        return new URLParams(protocol, release);
    }

    /**
     * Converts a ServiceEndpoint to a URL.
     * Copies all basic properties including service name, host, port and metadata.
     *
     * @param endpoint the service endpoint to convert
     * @return a new URL containing the endpoint's data
     */
    public static URL toURL(ServiceEndpoint endpoint) {
        Map<String, String> metadata = endpoint.getMetadata() == null ? new HashMap<>() : new HashMap<>(endpoint.getMetadata());
        String scheme = endpoint.getScheme();
        return new URL(scheme, endpoint.getHost(), endpoint.getPort(), endpoint.getService(), metadata);
    }

    @Getter
    public static class URLParams {

        private final String protocol;

        private final String release;

        public URLParams(String protocol, String release) {
            this.protocol = protocol;
            this.release = release;
        }
    }
}
