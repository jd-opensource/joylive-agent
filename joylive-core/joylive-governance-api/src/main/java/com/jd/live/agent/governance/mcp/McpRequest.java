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
package com.jd.live.agent.governance.mcp;

import java.util.Map;

/**
 * Represents a Model Context Protocol (MCP) request, providing access to various
 * request components including query parameters, headers, cookies, path variables,
 * and request body.
 * <p>
 * This interface abstracts the underlying HTTP request details and provides
 * a unified access pattern for MCP tool implementations.
 */
public interface McpRequest {

    /**
     * Returns all query parameters from the request.
     *
     * @return a map of query parameter names to their values
     */
    Map<String, ? extends Object> getQueries();

    /**
     * Returns a specific query parameter value by name.
     *
     * @param name the name of the query parameter
     * @return the value of the specified query parameter, or null if not present
     */
    default Object getQuery(String name) {
        if (name == null) {
            return null;
        }
        Map<String, ? extends Object> map = getQueries();
        return map == null ? null : map.get(name);
    }

    /**
     * Returns all HTTP headers from the request.
     *
     * @return a map of header names to their values
     */
    Map<String, ? extends Object> getHeaders();

    /**
     * Returns a specific HTTP header value by name.
     *
     * @param name the name of the header
     * @return the value of the specified header, or null if not present
     */
    default Object getHeader(String name) {
        if (name == null) {
            return null;
        }
        Map<String, ? extends Object> map = getHeaders();
        return map == null ? null : map.get(name);
    }

    /**
     * Returns all cookies from the request.
     *
     * @return a map of cookie names to their values
     */
    Map<String, ? extends Object> getCookies();

    /**
     * Returns a specific cookie value by name.
     *
     * @param name the name of the cookie
     * @return the value of the specified cookie, or null if not present
     */
    default Object getCookie(String name) {
        if (name == null) {
            return null;
        }
        Map<String, ? extends Object> map = getCookies();
        return map == null ? null : map.get(name);
    }

    /**
     * Returns all path variables from the request.
     *
     * @return a map of path variable names to their values
     */
    Map<String, Object> getPaths();

    /**
     * Returns a specific path variable value by name.
     *
     * @param name the name of the path variable
     * @return the value of the specified path variable, or null if not present
     */
    default Object getPath(String name) {
        if (name == null) {
            return null;
        }
        Map<String, Object> map = getPaths();
        return map == null ? null : map.get(name);
    }

    /**
     * Returns a specific value from the request body.
     *
     * @param name the name of the body parameter
     * @return the value of the specified body parameter, or null if not present
     */
    Object getBody(String name);

    /**
     * Returns a specific value from the request body.
     *
     * @return the value of the specified body parameter, or null if not present
     */
    Object getBody();
}
