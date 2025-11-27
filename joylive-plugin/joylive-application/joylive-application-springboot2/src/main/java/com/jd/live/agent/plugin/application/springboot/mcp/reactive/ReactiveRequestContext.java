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
package com.jd.live.agent.plugin.application.springboot.mcp.reactive;

import com.jd.live.agent.core.mcp.McpRequestContext.AbstractRequestContext;
import com.jd.live.agent.core.mcp.McpSession;
import com.jd.live.agent.core.mcp.McpToolInterceptor;
import com.jd.live.agent.core.mcp.McpToolMethod;
import com.jd.live.agent.core.mcp.version.McpVersion;
import com.jd.live.agent.core.openapi.spec.v3.OpenApi;
import com.jd.live.agent.core.parser.JsonSchemaParser;
import com.jd.live.agent.core.parser.ObjectConverter;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

import java.util.List;
import java.util.Map;

/**
 * Reactive implementation of MCP request context that works with Spring WebFlux.
 * Provides access to request/response information through ServerWebExchange.
 */
@Getter
public class ReactiveRequestContext extends AbstractRequestContext {

    /**
     * The Spring WebFlux server exchange containing request and response
     */
    private final ServerWebExchange exchange;

    @Builder
    public ReactiveRequestContext(McpSession session,
                                  Map<String, McpToolMethod> methods,
                                  Map<String, List<McpToolMethod>> paths,
                                  ObjectConverter converter,
                                  JsonSchemaParser jsonSchemaParser,
                                  McpVersion version,
                                  OpenApi openApi,
                                  McpToolInterceptor interceptor,
                                  ServerWebExchange exchange) {
        super(session, methods, paths, converter, jsonSchemaParser, version, openApi, interceptor);
        this.exchange = exchange;
    }

    /**
     * Gets a header value by name
     *
     * @param name The header name
     * @return The header value or null if not found
     */
    @Override
    public String getHeader(String name) {
        return name == null ? null : exchange.getRequest().getHeaders().getFirst(name);
    }

    /**
     * Gets all request headers
     *
     * @return Map of header names to values
     */
    @Override
    public Map<String, ? extends Object> getHeaders() {
        return exchange.getRequest().getHeaders();
    }

    /**
     * Gets all request cookies
     *
     * @return Map of cookie names to values
     */
    @Override
    public Map<String, ? extends Object> getCookies() {
        return exchange.getRequest().getCookies();
    }

    /**
     * Gets a cookie value by name
     *
     * @param name The cookie name
     * @return The cookie value or null if not found
     */
    @Override
    public Object getCookie(String name) {
        return name == null ? null : exchange.getRequest().getCookies().get(name);
    }

    /**
     * Gets a session attribute by name
     * Note: This method blocks to obtain the session
     *
     * @param name The attribute name
     * @return The attribute value or null if not found
     */
    @Override
    public Object getSessionAttribute(String name) {
        WebSession session = exchange.getSession().block();
        return session == null ? null : session.getAttribute(name);
    }

    /**
     * Gets a request attribute by name
     *
     * @param name The attribute name
     * @return The attribute value or null if not found
     */
    @Override
    public Object getRequestAttribute(String name) {
        return exchange.getAttribute(name);
    }
}
