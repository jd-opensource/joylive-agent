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
public interface HttpRequest extends ServiceRequest {

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
     * Returns the port number of the request.
     *
     * @return The port number.
     */
    int getPort();

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

    /**
     * Returns the values of a specific header.
     *
     * @param key The name of the header.
     * @return A list of values for the specified header, or null if the header does not exist.
     */
    default List<String> getHeaders(String key) {
        Map<String, List<String>> result = getHeaders();
        return result == null || key == null ? null : result.get(key);
    }

    /**
     * Returns the first value of a specific header.
     *
     * @param key The name of the header.
     * @return The first value of the specified header, or null if the header does not exist or has no values.
     */
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

    /**
     * Returns the value of a specific query parameter.
     *
     * @param key The name of the query parameter.
     * @return The value of the specified query parameter, or null if it does not exist.
     */
    String getQuery(String key);

    /**
     * Returns all cookies of the request as a map.
     *
     * @return A map of cookie names to their respective list of cookies.
     */
    Map<String, List<Cookie>> getCookies();

    /**
     * Returns the value of a specific cookie.
     *
     * @param key The name of the cookie.
     * @return The value of the specified cookie, or null if it does not exist.
     */
    String getCookie(String key);

    /**
     * Defines an interface for inbound HTTP requests.
     * <p>
     * This interface represents HTTP requests that are received by a service.
     * </p>
     *
     * @since 1.0.0
     */
    interface HttpInboundRequest extends HttpRequest, InboundRequest {

    }

    /**
     * Defines an interface for outbound HTTP requests.
     * <p>
     * This interface represents HTTP requests that are sent by a service.
     * </p>
     *
     * @author Zhiguo.Chen
     * @since 1.0.0
     */
    interface HttpOutboundRequest extends HttpRequest, OutboundRequest {

    }
}

