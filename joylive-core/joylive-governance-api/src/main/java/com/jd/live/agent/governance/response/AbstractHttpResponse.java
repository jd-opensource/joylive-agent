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

import com.jd.live.agent.core.util.http.HttpHeader;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.jd.live.agent.core.util.StringUtils.EMPTY_STRING;
import static java.util.Collections.EMPTY_MAP;

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
    protected Map<String, List<String>> cookies;

    /**
     * Lazily evaluated HTTP headers from the request.
     */
    protected Map<String, List<String>> headers;

    /**
     * The URI of the HTTP request.
     */
    protected URI uri;

    /**
     * Lazily evaluated port number of the request URI.
     */
    protected Integer port;

    /**
     * Lazily evaluated host of the request URI.
     */
    protected String host;

    /**
     * Lazily evaluated scheme of the request URI.
     */
    protected String schema;

    protected String contentType;

    /**
     * Constructs an instance of {@code AbstractHttpResponse} with the original response object.
     *
     * @param response The original response object.
     */
    public AbstractHttpResponse(T response) {
        this(response, (Supplier<ServiceError>) null, null);
    }

    /**
     * Constructs an instance of {@code AbstractHttpResponse} with the original response object.
     *
     * @param error     The original exception.
     * @param retryPredicate a custom predicate to evaluate retryability of the response
     */
    public AbstractHttpResponse(ServiceError error, ErrorPredicate retryPredicate) {
        this(null, error == null ? null : () -> error, retryPredicate);
    }

    /**
     * Creates a new instance of AbstractHttpResponse with the original response object.
     *
     * @param response       The original response object.
     * @param error          The original exception.
     * @param retryPredicate a custom predicate to evaluate retryability of the response
     */
    public AbstractHttpResponse(T response, ServiceError error, ErrorPredicate retryPredicate) {
        this(response, error == null ? null : () -> error, retryPredicate);
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
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public String getSchema() {
        if (schema == null) {
            String v = parseScheme();
            schema = v == null ? EMPTY_STRING : v;
        }
        return schema.isEmpty() ? null : schema;
    }

    @Override
    public int getPort() {
        if (port == null) {
            port = parsePort();
        }
        return port;
    }

    @Override
    public String getHost() {
        if (host == null) {
            String v = parseHost();
            host = v == null ? EMPTY_STRING : v;
        }
        return host.isEmpty() ? null : host;
    }

    @Override
    public String getPath() {
        return uri == null ? null : uri.getPath();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, List<String>> getHeaders() {
        if (headers == null) {
            Map<String, List<String>> map = parseHeaders();
            // not null
            headers = map == null ? EMPTY_MAP : headers;
        }
        return headers;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, List<String>> getCookies() {
        if (cookies == null) {
            Map<String, List<String>> map = parseCookies();
            // not null
            cookies = map == null ? EMPTY_MAP : map;
        }
        return cookies;
    }

    @Override
    public String getCookie(String key) {
        if (key == null) return null;
        List<String> values = getCookies().get(key);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    @Override
    public String getContentType() {
        if (contentType == null) {
            String header = getHeader(HttpHeader.CONTENT_TYPE);
            contentType = header == null ? EMPTY_STRING : header;
        }
        return contentType.isEmpty() ? null : contentType;
    }

    /**
     * Parses the port number from the request URI.
     *
     * @return The port number, or a default value if it cannot be parsed from the URI.
     */
    protected int parsePort() {
        int result = uri == null ? -1 : uri.getPort();
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

    @Override
    protected ServiceError parseError() {
        return ServiceError.build(this::getHeader);
    }

    /**
     * Parses the host from the request URI.
     *
     * @return The host, or a default value if it cannot be parsed from the URI.
     */
    protected String parseHost() {
        String result = uri == null ? null : uri.getHost();
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
        return uri == null ? null : uri.getScheme();
    }

    /**
     * Parses the cookies from the HTTP response headers.
     *
     * @return A map of cookie names to their corresponding values, or null if no cookies were found
     */
    protected Map<String, List<String>> parseCookies() {
        return null;
    }

    /**
     * Parses the headers from the HTTP response headers.
     *
     * @return A map of header names to their corresponding values, or null if no headers were found
     */
    protected Map<String, List<String>> parseHeaders() {
        return null;
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
