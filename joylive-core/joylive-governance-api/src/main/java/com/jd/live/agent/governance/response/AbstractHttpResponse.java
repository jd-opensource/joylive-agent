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
package com.jd.live.agent.governance.response;

import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * AbstractHttpResponse
 *
 * @since 1.0.0
 */
public abstract class AbstractHttpResponse<T> extends AbstractServiceResponse<T> implements HttpResponse {

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
     * Constructs an instance of {@code AbstractHttpResponse} with the original response object.
     *
     * @param response The original response object.
     */
    public AbstractHttpResponse(T response) {
        this(response, () -> null, null);
    }

    /**
     * Constructs an instance of {@code AbstractHttpResponse} with the original response object.
     *
     * @param error     The original exception.
     * @param retryPredicate a custom predicate to evaluate retryability of the response
     */
    public AbstractHttpResponse(ServiceError error, ErrorPredicate retryPredicate) {
        this(null, error, retryPredicate);
    }

    /**
     * Creates a new instance of AbstractHttpResponse with the original response object.
     *
     * @param response       The original response object.
     * @param error          The original exception.
     * @param retryPredicate a custom predicate to evaluate retryability of the response
     */
    public AbstractHttpResponse(T response, ServiceError error, ErrorPredicate retryPredicate) {
        this(response, () -> error, retryPredicate);
    }

    /**
     * Creates a new instance of AbstractHttpResponse with the original response object.
     *
     * @param response       The original response object.
     * @param errorSupplier  The error supplier.
     * @param retryPredicate a custom predicate to evaluate retryability of the response
     */
    public AbstractHttpResponse(T response, Supplier<ServiceError> errorSupplier, ErrorPredicate retryPredicate) {
        super(response, errorSupplier, retryPredicate);
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
        return headers == null ? null : headers.get();
    }

    @Override
    public String getHeader(String key) {
        if (key == null) return null;
        Map<String, List<String>> headers = getHeaders();
        List<String> values = headers == null ? null : headers.get(key);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    @Override
    public Map<String, List<String>> getCookies() {
        return cookies == null ? null : cookies.get();
    }

    @Override
    public String getCookie(String key) {
        if (key == null) return null;
        Map<String, List<String>> cookies = getCookies();
        List<String> values = cookies == null ? null : cookies.get(key);
        return values == null || values.isEmpty() ? null : values.get(0);
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


    public abstract static class AbstractHttpOutboundResponse<T> extends AbstractHttpResponse<T>
            implements HttpResponse.HttpOutboundResponse {

        public AbstractHttpOutboundResponse(T response) {
            super(response);
        }

        public AbstractHttpOutboundResponse(ServiceError error, ErrorPredicate retryPredicate) {
            super(error, retryPredicate);
        }

        public AbstractHttpOutboundResponse(T response, ServiceError error, ErrorPredicate retryPredicate) {
            super(response, error, retryPredicate);
        }

        public AbstractHttpOutboundResponse(T response, Supplier<ServiceError> errorSupplier, ErrorPredicate retryPredicate) {
            super(response, errorSupplier, retryPredicate);
        }
    }
}
