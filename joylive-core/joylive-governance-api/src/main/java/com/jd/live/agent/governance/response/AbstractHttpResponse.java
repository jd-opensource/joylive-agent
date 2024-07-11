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

import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.governance.request.Cookie;

import java.net.URI;
import java.util.List;
import java.util.Map;

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
    protected LazyObject<Map<String, List<Cookie>>> cookies;

    /**
     * Lazily evaluated, parsed query parameters from the HTTP request URL.
     */
    protected LazyObject<Map<String, List<String>>> queries;

    /**
     * Lazily evaluated HTTP headers from the request.
     */
    protected LazyObject<Map<String, List<String>>> headers;

    /**
     * The URI of the HTTP request.
     */
    protected URI uri;

    /**
     * Lazily evaluated port number of the request URI.
     */
    protected LazyObject<Integer> port;

    /**
     * Lazily evaluated host of the request URI.
     */
    protected LazyObject<String> host;

    /**
     * Lazily evaluated scheme of the request URI.
     */
    protected LazyObject<String> schema;

    /**
     * Constructs an instance of {@code AbstractHttpResponse} with the original response object.
     *
     * @param response The original response object.
     * @param throwable The original exception.
     */
    public AbstractHttpResponse(T response, Throwable throwable) {
        super(response, throwable);
        port = new LazyObject<>(this::parsePort);
        host = new LazyObject<>(this::parseHost);
        schema = new LazyObject<>(this::parseScheme);
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
        List<String> values = headers.get().get(key);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    @Override
    public String getQuery(String key) {
        if (key == null) return null;
        List<String> values = queries.get().get(key);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    @Override
    public Map<String, List<String>> getQueries() {
        return queries.get();
    }

    @Override
    public Map<String, List<Cookie>> getCookies() {
        return cookies.get();
    }

    @Override
    public String getCookie(String key) {
        if (key == null) return null;
        List<Cookie> values = cookies.get().get(key);
        return values == null || values.isEmpty() ? null : values.get(0).getValue();
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

        public AbstractHttpOutboundResponse(T response, Throwable throwable) {
            super(response, throwable);
        }
    }
}
