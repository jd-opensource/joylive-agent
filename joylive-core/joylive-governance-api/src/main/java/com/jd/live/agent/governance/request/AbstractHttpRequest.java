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
package com.jd.live.agent.governance.request;

import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.governance.exception.ErrorPolicy;
import lombok.Getter;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides an abstract base class for HTTP requests, implementing the {@link HttpRequest} interface.
 * <p>
 * This class encapsulates common properties and behaviors for HTTP requests, such as URI parsing, header management,
 * query parameter parsing, and cookie handling. It leverages lazy evaluation for efficient processing.
 * </p>
 *
 * @param <T> The type of the original request object this class wraps.
 */
public abstract class AbstractHttpRequest<T> extends AbstractServiceRequest<T> implements HttpRequest {

    /**
     * Key for the "Host" header in HTTP requests.
     */
    protected static final String HEAD_HOST_KEY = "Host";

    /**
     * Key for the "serviceGroup" header in HTTP requests.
     */
    protected static final String HEAD_GROUP_KEY = "serviceGroup";

    /**
     * Lazily evaluated, parsed cookies from the HTTP request.
     */
    protected UnsafeLazyObject<Map<String, List<String>>> cookies;

    /**
     * Lazily evaluated, parsed query parameters from the HTTP request URL.
     */
    protected UnsafeLazyObject<Map<String, List<String>>> queries;

    /**
     * Lazily evaluated HTTP headers from the request.
     */
    protected UnsafeLazyObject<Map<String, List<String>>> headers;

    /**
     * The URI of the HTTP request.
     */
    protected URI uri;

    /**
     * Lazily evaluated host of the request URI.
     */
    protected UnsafeLazyObject<Address> address;

    /**
     * Lazily evaluated scheme of the request URI.
     */
    protected UnsafeLazyObject<String> schema;

    /**
     * Constructs an instance of {@code AbstractHttpRequest} with the original request object.
     *
     * @param request The original request object.
     */
    public AbstractHttpRequest(T request) {
        super(request);
        address = new UnsafeLazyObject<>(this::parseAddress);
        schema = new UnsafeLazyObject<>(this::parseScheme);
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public String getSchema() {
        String result = schema.get();
        return result == null || result.isEmpty() ? null : result;
    }

    @Override
    public Integer getPort() {
        Address addr = address.get();
        return addr == null ? null : addr.getPort();
    }

    @Override
    public String getHost() {
        Address addr = address.get();
        return addr == null ? null : addr.getHost();
    }

    @Override
    public String getPath() {
        return uri.getPath();
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return headers.get();
    }

    @Override
    public Map<String, List<String>> getQueries() {
        return queries.get();
    }

    @Override
    public Map<String, List<String>> getCookies() {
        return cookies.get();
    }

    @Override
    public String getService() {
        return getHost();
    }

    @Override
    public String getGroup() {
        return getHeader(HEAD_GROUP_KEY);
    }

    @Override
    public String getMethod() {
        return getHttpMethod().name();
    }

    /**
     * Parses the address from the URI and header.
     *
     * @return the parsed address, or null if the host is invalid
     */
    protected Address parseAddress() {
        Address result = parseAddressByUrl();
        if (result == null) {
            result = parseAddressByHeader();
        }
        return result;
    }

    /**
     * Parses the address from the URL.
     *
     * @return the parsed address, or null if the host is invalid
     */
    protected Address parseAddressByUrl() {
        String host = uri.getHost();
        int port = uri.getPort();
        return validateHost(host) ? new Address(host, port < 0 ? null : port) : null;
    }

    /**
     * Parses the address from the HTTP header.
     *
     * @return the parsed address, or null if the host is invalid
     */
    protected Address parseAddressByHeader() {
        String header = getHeader(HEAD_HOST_KEY);
        if (header == null) {
            return null;
        }
        String host = header;
        int port = -1;
        int length = header.length();
        for (int i = length - 1; i > 0; i--) {
            switch (header.charAt(i)) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    continue;
                case ':':
                    host = header.substring(0, i);
                    // port
                    if (i < length - 1) {
                        try {
                            port = Integer.parseInt(header.substring(i + 1));
                        } catch (NumberFormatException ignore) {
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return validateHost(host) ? new Address(host, port < 0 ? null : port) : null;
    }

    /**
     * Validates the given host.
     *
     * @param host the host to validate
     * @return true if the host is valid, false otherwise
     */
    protected boolean validateHost(String host) {
        return host != null && !host.isEmpty();
    }

    /**
     * Parses the scheme (protocol) from the request URI.
     *
     * @return The scheme, or null if it cannot be determined.
     */
    protected String parseScheme() {
        return uri.getScheme();
    }

    @Getter
    protected static class Address {

        private final String host;

        private final Integer port;

        public Address(String host, Integer port) {
            this.host = host;
            this.port = port;
        }
    }

    /**
     * Represents an abstract base class for inbound HTTP requests.
     * <p>
     * This class is designed to handle requests received by a service. It extends {@link AbstractHttpRequest} to inherit HTTP-specific
     * properties and behaviors, such as URI parsing, header management, and query parameter handling. The class implements the
     * {@link HttpInboundRequest} interface, indicating it is intended for processing requests coming into a service.
     * </p>
     *
     * @param <T> The type of the original request object this class wraps.
     */
    public abstract static class AbstractHttpInboundRequest<T> extends AbstractHttpRequest<T>
            implements HttpInboundRequest {

        public AbstractHttpInboundRequest(T request) {
            super(request);
        }

        @Override
        protected boolean validateHost(String host) {
            if (host == null || host.isEmpty()) {
                return false;
            } else if (host.charAt(0) == '[') {
                // IPv6
                return false;
            } else {
                // Not IPv4
                return !Ipv4.isIpv4(host);
            }
        }
    }

    /**
     * Represents an abstract base class for outbound HTTP requests.
     * <p>
     * This class is tailored for handling requests sent from a service to another service or component. It inherits from
     * {@link AbstractHttpRequest}, leveraging common HTTP functionalities such as URI parsing, header and query parameter management,
     * and cookie handling. By implementing the {@link HttpOutboundRequest} interface, it specifies its role in representing
     * outbound HTTP communication.
     * </p>
     *
     * @param <T> The type of the original request object this class wraps.
     */
    public abstract static class AbstractHttpOutboundRequest<T> extends AbstractHttpRequest<T>
            implements HttpOutboundRequest {

        public AbstractHttpOutboundRequest(T request) {
            super(request);
        }

        @Override
        public void addErrorPolicy(ErrorPolicy policy) {
            if (policy != null && policy.requireResponseBody()) {
                getAttributeIfAbsent(KEY_ERROR_POLICY, k -> new HashSet<ErrorPolicy>()).add(policy);
            }
        }

        @Override
        public Set<ErrorPolicy> removeErrorPolicies() {
            return removeAttribute(KEY_ERROR_POLICY);
        }
    }
}
