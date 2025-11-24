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
package com.jd.live.agent.core.mcp;

import com.jd.live.agent.core.mcp.version.McpVersion;
import com.jd.live.agent.core.openapi.spec.v3.OpenApi;
import com.jd.live.agent.core.parser.JsonSchemaParser;
import com.jd.live.agent.core.parser.ObjectConverter;
import lombok.Getter;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Context interface for MCP request parameter conversion.
 *
 * <p>Provides access to components needed for processing MCP requests,
 * including parameter conversion, method resolution, and parameter parsing.
 *
 * @see ObjectConverter
 */
public interface McpRequestContext extends ObjectConverter {

    /**
     * Gets the converter for transforming request parameters.
     *
     * @return the object converter instance
     */
    ObjectConverter getConverter();

    /**
     * Returns the JSON schema parser instance.
     *
     * @return A parser that can generate JSON schema definitions
     */
    JsonSchemaParser getJsonSchemaParser();

    /**
     * Retrieves the current MCP (Model Control Protocol) version.
     *
     * @return The MCP version implementation currently in use
     */
    McpVersion getVersion();

    /**
     * Retrieves all available MCP tool methods.
     *
     * @return a map of method names to their corresponding McpToolMethod objects
     */
    Map<String, McpToolMethod> getMethods();

    /**
     * Retrieves all available MCP tool methods.
     *
     * @return a map of method names to their corresponding McpToolMethod objects
     */
    Map<String, McpToolMethod> getPaths();

    /**
     * Retrieves a tool method by its name.
     *
     * @param methodName the name of the method to retrieve
     * @return the corresponding tool method
     */
    default McpToolMethod getToolMethodByName(String methodName) {
        Map<String, McpToolMethod> methods = getMethods();
        return methodName == null || methods == null ? null : methods.get(methodName);
    }

    /**
     * Retrieves a tool method by its path identifier.
     *
     * @param path The path string that identifies the tool method
     * @return The corresponding McpToolMethod object
     */
    default McpToolMethod getToolMethodByPath(String path) {
        Map<String, McpToolMethod> methods = getPaths();
        return methods == null ? null : methods.get(path);
    }

    /**
     * Retrieves the value of the specified HTTP header.
     *
     * @param name the name of the header to retrieve
     * @return the header value, or null if not present
     */
    Object getHeader(String name);

    Map<String, ? extends Object> getHeaders();

    Map<String, ? extends Object> getCookies();

    Object getCookie(String name);

    Object getSessionAttribute(String name);

    Object getRequestAttribute(String name);

    /**
     * Adds a cookie with the specified name and value.
     *
     * @param name  the cookie name
     * @param value the cookie value
     */
    void addCookie(String name, String value);

    /**
     * Retrieves OpenApi
     *
     * @return open api insance
     */
    default OpenApi getOpenApi() {
        return null;
    }

    @Override
    default <T> T convert(Object source, Class<T> type) {
        return getConverter().convert(source, type);
    }

    @Override
    default Object convert(Object source, Type type) {
        return getConverter().convert(source, type);
    }

    @Getter
    abstract class AbstractRequestContext implements McpRequestContext {

        private final Map<String, McpToolMethod> methods;

        private final Map<String, McpToolMethod> paths;

        private final ObjectConverter converter;

        private final JsonSchemaParser jsonSchemaParser;

        private final McpVersion version;

        private final OpenApi openApi;

        public AbstractRequestContext(Map<String, McpToolMethod> methods,
                                      Map<String, McpToolMethod> paths,
                                      ObjectConverter converter,
                                      JsonSchemaParser jsonSchemaParser,
                                      McpVersion version,
                                      OpenApi openApi) {
            this.methods = methods;
            this.paths = paths;
            this.converter = converter;
            this.jsonSchemaParser = jsonSchemaParser;
            this.version = version;
            this.openApi = openApi;
        }
    }
}
