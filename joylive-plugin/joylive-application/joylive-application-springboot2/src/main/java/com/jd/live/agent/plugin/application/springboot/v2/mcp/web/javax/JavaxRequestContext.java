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
package com.jd.live.agent.plugin.application.springboot.v2.mcp.web.javax;

import com.jd.live.agent.core.parser.JsonSchemaParser;
import com.jd.live.agent.core.parser.ObjectConverter;
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.core.mcp.McpRequestContext.AbstractRequestContext;
import com.jd.live.agent.core.mcp.McpToolMethod;
import com.jd.live.agent.core.mcp.version.McpVersion;
import com.jd.live.agent.core.openapi.spec.v3.OpenApi;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;
import static com.jd.live.agent.core.util.http.HttpUtils.parseCookie;
import static com.jd.live.agent.core.util.http.HttpUtils.parseHeader;

/**
 * Javax Servlet API implementation of MCP request context.
 * Provides access to request/response information through Javax Servlet API.
 */
@Getter
public class JavaxRequestContext extends AbstractRequestContext {

    /**
     * Spring web request object
     */
    private final WebRequest webRequest;

    /**
     * Javax HTTP servlet request
     */
    private final HttpServletRequest httpRequest;

    /**
     * Javax HTTP servlet response
     */
    private final HttpServletResponse httpResponse;

    /**
     * Lazily initialized map of request headers
     */
    private final LazyObject<Map<String, List<String>>> headers;

    /**
     * Lazily initialized map of request cookies
     */
    private final LazyObject<Map<String, List<String>>> cookies;

    /**
     * Creates a new Javax request context
     *
     * @param methods          MCP tool methods mapped by name
     * @param paths            MCP tool methods mapped by path
     * @param converter        Object converter for parameter conversion
     * @param jsonSchemaParser JSON schema parser for parameter validation
     * @param version          MCP version information
     * @param openApi          OpenAPI specification supplier
     * @param webRequest       Spring web request object
     * @param httpRequest      Javax HTTP servlet request
     * @param httpResponse     Javax HTTP servlet response
     */
    @Builder
    public JavaxRequestContext(Map<String, McpToolMethod> methods,
                               Map<String, McpToolMethod> paths,
                               ObjectConverter converter,
                               JsonSchemaParser jsonSchemaParser,
                               McpVersion version,
                               Supplier<OpenApi> openApi,
                               WebRequest webRequest,
                               HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        super(methods, paths, converter, jsonSchemaParser, version, openApi);
        this.webRequest = webRequest;
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
        this.headers = LazyObject.of(() -> parseHeader(httpRequest.getHeaderNames(), v -> httpRequest.getHeaders(v)));
        this.cookies = LazyObject.of(() -> parseCookie(httpRequest.getCookies(), c -> c.getName(), c -> c.getValue()));
    }

    /**
     * Gets a header value by name
     *
     * @param name The header name
     * @return The header value or null if not found
     */
    @Override
    public Object getHeader(String name) {
        return name == null ? null : getHeaders().get(name);
    }

    /**
     * Gets all request headers
     *
     * @return Map of header names to values
     */
    @Override
    public Map<String, ? extends Object> getHeaders() {
        return headers.get();
    }

    /**
     * Gets all request cookies
     *
     * @return Map of cookie names to values
     */
    @Override
    public Map<String, ? extends Object> getCookies() {
        return cookies.get();
    }

    /**
     * Gets a cookie value by name
     *
     * @param name The cookie name
     * @return The cookie value or null if not found
     */
    @Override
    public Object getCookie(String name) {
        return name == null ? null : getCookies().get(name);
    }

    /**
     * Adds a cookie to the response
     *
     * @param name The cookie name
     * @param value The cookie value
     */
    @Override
    public void addCookie(String name, String value) {
        if (!isEmpty(name) && !isEmpty(value)) {
            httpResponse.addCookie(new Cookie(name, value));
        }
    }

    /**
     * Gets a session attribute by name
     *
     * @param name The attribute name
     * @return The attribute value or null if not found
     */
    @Override
    public Object getSessionAttribute(String name) {
        return httpRequest.getSession().getAttribute(name);
    }

    /**
     * Gets a request attribute by name
     *
     * @param name The attribute name
     * @return The attribute value or null if not found
     */
    @Override
    public Object getRequestAttribute(String name) {
        return httpRequest.getAttribute(name);
    }
}
