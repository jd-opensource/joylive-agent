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

import java.net.URI;
import java.util.List;
import java.util.Map;

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
     * Lazily evaluated port number of the request URI.
     */
    protected UnsafeLazyObject<Integer> port;

    /**
     * Lazily evaluated host of the request URI.
     */
    protected UnsafeLazyObject<String> host;

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
        port = new UnsafeLazyObject<>(this::parsePort);
        host = new UnsafeLazyObject<>(this::parseHost);
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
    public int getPort() {
        return port.get();
    }

    @Override
    public String getHost() {
        String result = host.get();
        return result == null || result.isEmpty() ? null : result;
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
     * Parses the port number from the request URI.
     *
     * @return The port number, or a default value if it cannot be parsed from the URI.
     */
    protected int parsePort() {
        int result = uri.getPort();
        if (result < 0) {
            result = parsePortByHeader();
        }
        return result;
    }

    /**
     * Parses the port number from the "Host" header when it is not directly available from the URI.
     *
     * @return The port number parsed from the header, or -1 if it cannot be determined.
     */
    protected int parsePortByHeader() {
        int result = -1;
        String header = getHeader(HEAD_HOST_KEY);
        if (header != null) {
            char ch;
            for (int i = header.length() - 1; i > 0; i--) {
                ch = header.charAt(i);
                if (ch == ':') {
                    try {
                        result = Integer.parseInt(header.substring(i + 1));
                    } catch (NumberFormatException ignore) {
                    }
                    break;
                } else if (!Character.isDigit(ch)) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Parses the host from the request URI.
     *
     * @return The host, or a default value if it cannot be parsed from the URI.
     */
    protected String parseHost() {
        String result = uri.getHost();
        if (result == null) {
            result = parseHostByHeader();
        }
        return result;
    }

    /**
     * Parses the host from the "Host" header when it is not directly available from the URI.
     *
     * @return The host parsed from the header, or null if it cannot be determined.
     */
    protected String parseHostByHeader() {
        String result = null;
        String header = getHeader(HEAD_HOST_KEY);
        if (header != null) {
            char ch;
            for (int i = header.length() - 1; i > 0; i--) {
                ch = header.charAt(i);
                if (ch == ':') {
                    result = header.substring(0, i);
                    break;
                } else if (!Character.isDigit(ch)) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Parses the scheme (protocol) from the request URI.
     *
     * @return The scheme, or null if it cannot be determined.
     */
    protected String parseScheme() {
        return uri.getScheme();
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
    }
}
