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
package com.jd.live.agent.core.util;

import lombok.Builder;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * Represents a URI (Uniform Resource Identifier) with components like schema, host, port, path, and parameters.
 * Provides methods to construct and modify the URI.
 */
public class URI {

    @Getter
    private String schema;

    @Getter
    private String host;

    @Getter
    private Integer port;

    @Getter
    private String path;

    private Map<String, String> parameters;

    private String url;

    private Long id;

    /**
     * Default constructor.
     */
    public URI() {
    }

    /**
     * Constructs a URI with the specified schema, host, path, and parameters.
     *
     * @param schema     the URI schema (e.g., "http", "https").
     * @param host       the host name or IP address.
     * @param path       the path component of the URI.
     * @param parameters the query parameters as a map.
     */
    @Builder
    public URI(String schema, String host, String path, Map<String, String> parameters) {
        this.schema = schema;
        this.host = host;
        this.path = path;
        this.parameters = parameters;
    }

    /**
     * Sets the host component of the URI.
     *
     * @param host the new host name or IP address.
     * @return a new URI instance with the updated host.
     */
    public URI host(String host) {
        return new URI(schema, host, path, parameters);
    }

    /**
     * Sets the port component of the URI.
     *
     * @param port the new port number.
     * @return a new URI instance with the updated port.
     */
    public URI port(Integer port) {
        return new URI(schema, host, path, parameters);
    }

    /**
     * Sets the path component of the URI.
     *
     * @param path the new path.
     * @return a new URI instance with the updated path.
     */
    public URI path(String path) {
        return new URI(schema, host, path, parameters);
    }

    /**
     * Adds or updates a single query parameter.
     *
     * @param key   the parameter key.
     * @param value the parameter value.
     * @return a new URI instance with the updated parameters.
     */
    public URI parameter(String key, String value) {
        if (key == null) {
            return this;
        }
        Map<String, String> newParameters = parameters == null ? new HashMap<>() : new HashMap<>(parameters);
        newParameters.put(key, value);
        return new URI(schema, host, path, newParameters);
    }

    /**
     * Adds or updates multiple query parameters.
     *
     * @param keyValues an array of key-value pairs.
     * @return a new URI instance with the updated parameters.
     */
    public URI parameters(String... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return this;
        }
        Map<String, String> newParameters = parameters == null ? new HashMap<>() : new HashMap<>(parameters);
        int pairs = keyValues.length / 2;
        for (int i = 0; i < pairs; i++) {
            int keyIdx = i * 2;
            int valueIdx = keyIdx + 1;
            newParameters.put(keyValues[keyIdx], valueIdx >= keyValues.length ? null : keyValues[valueIdx]);
        }
        return new URI(schema, host, path, newParameters);
    }

    /**
     * Retrieves the value of a specific query parameter.
     *
     * @param key the parameter key.
     * @return the parameter value, or {@code null} if the parameter does not exist.
     */
    public String getParameter(String key) {
        return parameters == null || key == null ? null : parameters.get(key);
    }

    /**
     * Constructs the full URI string.
     *
     * @return the full URI string.
     */
    public String getUri() {
        if (url == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(schema).append("://").append(host);
            if (port != null) {
                sb.append(":").append(port);
            }
            if (path != null) {
                sb.append("/").append(path);
            }
            if (parameters != null && !parameters.isEmpty()) {
                sb.append('?');
                int count = 0;
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    if (count++ > 0) {
                        sb.append('&');
                    }
                    sb.append(entry.getKey());
                    if (entry.getValue() != null) {
                        sb.append("=").append(entry.getValue());
                    }
                }
            }
            url = sb.toString();
        }
        return url;
    }

    /**
     * Computes and retrieves a unique identifier for the URI based on its content.
     *
     * @return the unique identifier for the URI.
     */
    public Long getId() {
        if (id == null) {
            CRC32 crc32 = new CRC32();
            byte[] bytes = getUri().getBytes(StandardCharsets.UTF_8);
            crc32.update(bytes, 0, bytes.length);
            id = Math.abs(crc32.getValue());
        }
        return id;
    }

    @Override
    public String toString() {
        return getUri();
    }
}
