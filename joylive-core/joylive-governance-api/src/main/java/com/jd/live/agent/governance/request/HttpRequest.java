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

import com.jd.live.agent.core.util.http.HttpMethod;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Defines an interface for HTTP requests, extending the functionality of {@link ServiceRequest}.
 * <p>
 * This interface provides methods for accessing various components of an HTTP request, such as URI, schema, port, host,
 * path, HTTP method, headers, queries, and cookies. It serves as a base for more specific HTTP request types, including
 * inbound and outbound requests.
 * </p>
 */
public interface HttpRequest extends ServiceRequest, Portable {

    /**
     * Returns the URI of the request.
     *
     * @return The URI object representing the request's URI.
     */
    URI getURI();

    /**
     * Returns the schema of the request, such as "http" or "https".
     *
     * @return The schema as a string.
     */
    String getSchema();

    /**
     * Returns the host name of the request.
     *
     * @return The host name as a string.
     */
    String getHost();

    /**
     * Returns the path of the request.
     *
     * @return The path as a string.
     */
    String getPath();

    /**
     * Returns the HTTP method of the request.
     *
     * @return The HTTP method as an instance of {@link HttpMethod}.
     */
    HttpMethod getHttpMethod();

    /**
     * Returns all headers of the request as a map.
     *
     * @return A map of header names to their respective list of values.
     */
    Map<String, List<String>> getHeaders();

    @Override
    default List<String> getHeaders(String key) {
        Map<String, List<String>> result = getHeaders();
        return result == null || key == null ? null : result.get(key);
    }

    @Override
    default String getHeader(String key) {
        List<String> headers = getHeaders(key);
        return headers == null || headers.isEmpty() ? null : headers.get(0);
    }

    /**
     * Returns all query parameters of the request as a map.
     *
     * @return A map of query parameter names to their respective list of values.
     */
    Map<String, List<String>> getQueries();

    @Override
    default List<String> getQueries(String key) {
        Map<String, List<String>> result = getQueries();
        return result == null || key == null ? null : result.get(key);
    }

    @Override
    default String getQuery(String key) {
        List<String> queries = getQueries(key);
        return queries == null || queries.isEmpty() ? null : queries.get(0);
    }

    /**
     * Returns all cookies of the request as a map.
     *
     * @return A map of cookie names to their respective list of cookies.
     */
    Map<String, List<String>> getCookies();

    @Override
    default List<String> getCookies(String key) {
        Map<String, List<String>> result = getCookies();
        return result == null || key == null ? null : result.get(key);
    }

    @Override
    default String getCookie(String key) {
        List<String> cookies = getCookies(key);
        return cookies == null || cookies.isEmpty() ? null : cookies.get(0);
    }

    /**
     * Defines an interface for inbound HTTP requests.
     * <p>
     * This interface represents HTTP requests that are received by a service.
     * </p>
     *
     * @since 1.0.0
     */
    interface HttpInboundRequest extends HttpRequest, InboundRequest {

        @Override
        default String getClientIp() {
            String ipAddress = getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = getHeader("HTTP_CLIENT_IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                return null;
            } else {
                int pos = ipAddress.indexOf(',');
                if (pos > 0) {
                    return ipAddress.substring(0, pos);
                }
                return ipAddress;
            }
        }
    }

    /**
     * Defines an interface for outbound HTTP requests.
     * <p>
     * This interface represents HTTP requests that are sent by a service.
     * </p>
     *
     * @since 1.0.0
     */
    interface HttpOutboundRequest extends HttpRequest, OutboundRequest {

        /**
         * Retrieves an expression that describes the target host for this request.
         *
         * @return A {@link String} representing the host expression for this request, or {@code null} if no expression
         *         is defined.
         */
        default String getForwardHostExpression() {
            return null;
        }

        /**
         * Forwards this request to the specified host.
         *
         * @param host The target host to which the request should be forwarded.
         */
        default void forward(String host) {

        }

    }
}

