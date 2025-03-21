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

    private URI(String schema, String host, Integer port, String path, Map<String, String> parameters, String url) {
        this.schema = schema;
        this.host = host;
        this.port = port;
        this.path = path;
        this.parameters = parameters;
        this.url = url;
    }

    /**
     * Constructs a URI with the specified schema, host, path, and parameters.
     *
     * @param schema     the URI schema (e.g., "http", "https").
     * @param host       the host name or IP address.
     * @param port       the port.
     * @param path       the path component of the URI.
     * @param parameters the query parameters as a map.
     */
    @Builder
    public URI(String schema, String host, Integer port, String path, Map<String, String> parameters) {
        this(schema, host, port, path, parameters, null);
    }

    /**
     * Sets the schema component of the URI.
     *
     * @param schema the new schema.
     * @return a new URI instance with the updated schema.
     */
    public URI schema(String schema) {
        return new URI(schema, host, port, path, parameters);
    }

    /**
     * Sets the host component of the URI.
     *
     * @param host the new host name or IP address.
     * @return a new URI instance with the updated host.
     */
    public URI host(String host) {
        return new URI(schema, host, port, path, parameters);
    }

    /**
     * Sets the port component of the URI.
     *
     * @param port the new port number.
     * @return a new URI instance with the updated port.
     */
    public URI port(Integer port) {
        return new URI(schema, host, port, path, parameters);
    }

    /**
     * Sets the path component of the URI.
     *
     * @param path the new path.
     * @return a new URI instance with the updated path.
     */
    public URI path(String path) {
        return new URI(schema, host, port, path, parameters);
    }

    /**
     * Adds or updates a single query parameter.
     *
     * @param key   the parameter key.
     * @param value the parameter value.
     * @return a new URI instance with the updated parameters.
     */
    public URI parameter(String key, String value) {
        if (key == null || key.isEmpty()) {
            return this;
        }
        int size = parameters == null ? 0 : parameters.size();
        Map<String, String> newParameters = size == 0 ? new HashMap<>(size + 1) : new HashMap<>(parameters);
        newParameters.put(key, value);
        String newUrl = url;
        if (newUrl != null) {
            // improve performance
            int valueLen = value == null ? 0 : value.length();
            StringBuilder builder = new StringBuilder(newUrl.length() + key.length() + valueLen + 2)
                    .append(newUrl)
                    .append(size > 0 ? '&' : '?')
                    .append(key);
            if (valueLen > 0) {
                builder.append('=').append(value);
            }
            newUrl = builder.toString();
        }
        return new URI(schema, host, port, path, newParameters, newUrl);
    }

    public String getAddress() {
        if (port == null) {
            return host;
        } else {
            return host + ":" + port;
        }
    }

    public String getAddress(boolean withSchema) {
        if (!withSchema && schema == null) {
            return getAddress();
        }
        StringBuilder sb = new StringBuilder(64);
        if (schema != null) {
            sb.append(schema).append("://");
        }
        sb.append(host);
        if (port != null) {
            sb.append(":").append(port);
        }
        return sb.toString();
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
            StringBuilder sb = new StringBuilder(128);
            if (schema != null) {
                sb.append(schema).append("://");
            }
            sb.append(host);
            if (port != null) {
                sb.append(":").append(port);
            }
            if (path != null) {
                if (path.startsWith("/")) {
                    sb.append(path);
                } else {
                    sb.append("/").append(path);
                }
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

    /**
     * Parses a given URI string into a {@link URI} object. This method processes the input string to extract
     * the protocol, host, port, path, and query parameters.
     *
     * @param uri The URI string to be parsed.
     * @return A {@link URI} object representing the parsed components of the input string, or {@code null} if the input is {@code null} or empty.
     */
    public static URI parse(String uri) {
        String url = uri == null ? null : uri.trim();
        if (uri == null || uri.isEmpty()) {
            return null;
        }
        String protocol = null;
        String host = null;
        Integer port = null;
        String path = null;
        Map<String, String> parameters = null;

        int i = url.indexOf('?');
        if (i >= 0) {
            // parameter
            if (i < url.length() - 1) {
                String[] parts = url.substring(i + 1).split("&");
                parameters = new HashMap<>(10);
                for (String part : parts) {
                    part = part.trim();
                    if (!part.isEmpty()) {
                        int j = part.indexOf('=');
                        String name = j > 0 ? part.substring(0, j) : part;
                        String value = j > 0 && j < part.length() - 1 ? part.substring(j + 1) : "";
                        parameters.put(name, value);
                    }
                }
            }
            url = url.substring(0, i);
        }
        i = url.indexOf("://");
        if (i > 0) {
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        }
        i = url.indexOf("/");
        if (i >= 0) {
            path = url.substring(i);
            url = url.substring(0, i);
        }
        i = url.lastIndexOf(':');
        if (i > 0 && i < url.length() - 1) {
            try {
                port = Integer.parseInt(url.substring(i + 1));
                host = url.substring(0, i);
            } catch (NumberFormatException e) {
                // Handle invalid port number
                port = null;
                host = url;  // Fallback to full URL as host
            }
        } else {
            host = url;
        }
        return new URI(protocol, host, port, path, parameters);
    }

    /**
     * Parses the host from a given URI.
     *
     * @param uri the URI to parse
     * @return the host part of the URI, or null if the URI is invalid or does not contain a host
     */
    public static String parseHost(String uri) {
        String url = uri == null ? null : uri.trim();
        if (uri == null || uri.isEmpty()) {
            return null;
        }

        int start = url.indexOf("://");
        int end = -1;

        char c;
        for (int i = (start >= 0 ? start + 3 : 0); i < url.length(); i++) {
            c = url.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                end = i;
                break;
            }
        }
        if (start >= 0) {
            url = url.substring(start + 3, end >= 0 ? end : url.length());
        } else {
            url = end >= 0 ? url.substring(0, end) : uri;
        }
        if (url.isEmpty()) {
            return null;
        } else if (url.charAt(0) == '[') {
            // ipv6
            int pos = url.lastIndexOf(']');
            if (pos > 0) {
                return url.substring(1, pos);
            }
            return null;
        } else {
            // max port 65535
            int max = url.length() - 1;
            int min = max - 5;
            for (int i = max; i >= min; i--) {
                if (url.charAt(i) == ':') {
                    return url.substring(0, i);
                }
            }
            return url;
        }
    }

}
