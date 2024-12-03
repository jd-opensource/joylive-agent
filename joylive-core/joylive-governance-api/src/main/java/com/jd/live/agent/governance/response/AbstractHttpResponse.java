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

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.governance.exception.ErrorPolicy;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * Lazily evaluated exception names of the request.
     */
    protected UnsafeLazyObject<Set<String>> exceptionNames;

    /**
     * Lazily evaluated exception message of the request.
     */
    protected UnsafeLazyObject<String> exceptionMessage;

    /**
     * Constructs an instance of {@code AbstractHttpResponse} with the original response object.
     *
     * @param response The original response object.
     */
    public AbstractHttpResponse(T response) {
        this(response, null, null, null, null);
    }

    /**
     * Constructs an instance of {@code AbstractHttpResponse} with the original response object.
     *
     * @param error     The original exception.
     * @param predicate A predicate used to determine if the response should be considered an error.
     */
    public AbstractHttpResponse(ServiceError error, ErrorPredicate predicate) {
        this(null, error, predicate, null, null);
    }

    /**
     * Creates a new instance of AbstractHttpResponse with the original response object.
     *
     * @param response  The original response object.
     * @param error     The original exception.
     * @param predicate A predicate used to determine if the response should be considered an error.
     */
    public AbstractHttpResponse(T response, ServiceError error, ErrorPredicate predicate, Supplier<String> namesSupplier, Supplier<String> messageSupplier) {
        super(response, error, predicate);
        port = new UnsafeLazyObject<>(this::parsePort);
        host = new UnsafeLazyObject<>(this::parseHost);
        schema = new UnsafeLazyObject<>(this::parseScheme);
        exceptionNames = new UnsafeLazyObject<>(() -> this.parseExceptionNames(namesSupplier));
        exceptionMessage = new UnsafeLazyObject<>(() -> this.decodeExceptionMessage(messageSupplier));
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

    @Override
    public String getExceptionMessage() {
        return exceptionMessage == null ? null : exceptionMessage.get();
    }

    @Override
    public Set<String> getExceptionNames() {
        return exceptionNames == null ? null : exceptionNames.get();
    }

    /**
     *  parse exception names by namesSupplier or header
     * @param namesSupplier exception names supplier
     * @return exception names set
     */
    protected Set<String> parseExceptionNames(Supplier<String> namesSupplier) {
        String exceptionNames = getHeader(Constants.EXCEPTION_NAMES_LABEL);
        if (exceptionNames == null && namesSupplier != null) {
            exceptionNames = namesSupplier.get();
        }

        return ErrorPolicy.parseExceptionNames(exceptionNames, Constants.EXCEPTION_NAMES_SEPARATOR);
    }

    /**
     * decode exception message by messageSupplier or header
     * @param messageSupplier exception message supplier
     * @return decoded message
     */
    protected String decodeExceptionMessage(Supplier<String> messageSupplier) {
        String exceptionMessage = getHeader(Constants.EXCEPTION_MESSAGE_LABEL);
        if (exceptionMessage == null && messageSupplier != null) {
            exceptionMessage = messageSupplier.get();
        }

        if (exceptionMessage == null) {
            return null;
        }

        String decode = null;
        try {
            decode = URLDecoder.decode(exceptionMessage, Constants.EXCEPTION_NAMES_CODEC_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        return decode;
    }

    public abstract static class AbstractHttpOutboundResponse<T> extends AbstractHttpResponse<T>
            implements HttpResponse.HttpOutboundResponse {

        public AbstractHttpOutboundResponse(T response) {
            super(response);
        }

        public AbstractHttpOutboundResponse(T response, Supplier<String> exceptionNames, Supplier<String> exceptionMessage) {
            super(response, null, null, exceptionNames, exceptionMessage);
        }

        public AbstractHttpOutboundResponse(ServiceError error, ErrorPredicate predicate) {
            super(error, predicate);
        }

        public AbstractHttpOutboundResponse(T response, ServiceError error, ErrorPredicate predicate) {
            super(response, error, predicate, null, null);
        }
    }
}
