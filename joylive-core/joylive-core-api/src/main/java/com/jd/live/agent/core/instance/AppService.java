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
package com.jd.live.agent.core.instance;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Represents the service details within an application context.
 * This class encapsulates information about a service, including its name,
 * role (e.g., whether it acts as a gateway), namespace, group, and protocol.
 * It also manages service-specific metadata. The {@code AppService} class
 * provides methods to access service metadata and to check if the service
 * operates as a gateway.
 */
@Getter
@Setter
public class AppService {

    /**
     * Name of the service.
     */
    private String name;

    /**
     * Role of the service in terms of gateway functionality.
     */
    private GatewayRole gateway = GatewayRole.NONE;

    /**
     * Namespace the service belongs to.
     */
    private String namespace;

    /**
     * Group the service is part of.
     */
    private String group;

    /**
     * Communication protocol used by the service.
     */
    private String protocol;

    private Integer weight;

    private Integer warmupDuration;

    /**
     * Metadata associated with the service.
     */
    private Map<String, String> meta;

    /**
     * Default constructor for creating an instance of AppService.
     */
    public AppService() {
    }

    /**
     * Constructs a new AppService with the specified name.
     *
     * @param name The service name.
     */
    public AppService(String name) {
        this.name = name;
    }

    /**
     * Constructs a new AppService with the specified properties.
     *
     * @param name      The service name.
     * @param gateway   The gateway role.
     * @param namespace The service namespace.
     * @param group     The service group.
     * @param protocol  The service communication protocol.
     * @param meta      The service metadata.
     */
    @Builder
    public AppService(String name, GatewayRole gateway, String namespace, String group, String protocol, Map<String, String> meta) {
        this.name = name;
        this.gateway = gateway;
        this.namespace = namespace;
        this.group = group;
        this.protocol = protocol;
        this.meta = meta;
    }

    /**
     * Retrieves a metadata value associated with the specified key.
     *
     * @param key The metadata key to retrieve the value for.
     * @return The metadata value, or {@code null} if the key does not exist or metadata is not defined.
     */
    public String getMeta(String key) {
        return meta == null || key == null ? null : meta.get(key);
    }

    /**
     * Checks if the service is designated as a gateway.
     *
     * @return {@code true} if the service role is not {@code GatewayRole.NONE}, indicating it acts as a gateway; otherwise, {@code false}.
     */
    public boolean isGateway() {
        return gateway != GatewayRole.NONE;
    }

    public boolean isFrontGateway() {
        return gateway == GatewayRole.FRONTEND;
    }

}

