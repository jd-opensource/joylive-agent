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
package com.jd.live.agent.plugin.application.springboot.mcp.web.jakarta;

import com.jd.live.agent.core.mcp.McpRequest;
import com.jd.live.agent.core.mcp.McpRequestContext;
import com.jd.live.agent.core.mcp.McpToolParameter.Location;
import com.jd.live.agent.core.mcp.McpToolParameter.McpToolParameterBuilder;
import com.jd.live.agent.plugin.application.springboot.mcp.AbstractMcpToolScanner;
import com.jd.live.agent.plugin.application.springboot.mcp.converter.OptionalConverter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.context.request.WebRequest;

import java.lang.reflect.Type;
import java.security.Principal;
import java.util.Locale;
import java.util.Optional;

/**
 * Scanner for MCP tools based on Spring MVC controllers using Jakarta Servlet API.
 * This implementation handles Jakarta Servlet-specific parameter types and provides
 * system parameter resolution for Jakarta Servlet objects.
 */
public class JakartaWebMcpToolScanner extends AbstractMcpToolScanner {

    /**
     * Creates a new Jakarta Web MCP tool scanner
     *
     * @param beanFactory The Spring bean factory to use for bean resolution
     */
    public JakartaWebMcpToolScanner(ConfigurableListableBeanFactory beanFactory) {
        super(beanFactory);
    }

    /**
     * Configures parameter wrappers for special types.
     * Currently handles Optional types by extracting their actual type parameters
     * and applying appropriate converters.
     *
     * @param builder The parameter builder to configure
     * @return The configured parameter builder
     */
    @Override
    protected McpToolParameterBuilder configureWrapper(McpToolParameterBuilder builder) {
        // convert optional
        if (builder.type() == Optional.class) {
            Type actualType = getActualType(builder.genericType());
            if (actualType != null) {
                return builder.actualType(actualType).wrapper(OptionalConverter.INSTANCE);
            }
        }
        return builder;
    }

    /**
     * Configures system parameters for Jakarta Servlet-specific types.
     * Maps various Jakarta Servlet and Spring Web types to their appropriate system parameter resolvers.
     *
     * @param builder The parameter builder to configure
     * @return The configured parameter builder
     */
    @Override
    protected McpToolParameterBuilder configureSystemParam(McpToolParameterBuilder builder) {
        if (builder.isAssignableTo(WebRequest.class)) {
            return builder.location(Location.SYSTEM).convertable(false).parser(this::getWebRequest);
        } else if (builder.isType(HttpServletRequest.class)) {
            return builder.location(Location.SYSTEM).convertable(false).parser(this::getHttpRequest);
        } else if (builder.isType(HttpServletResponse.class)) {
            return builder.location(Location.SYSTEM).convertable(false).parser(this::getHttpResponse);
        } else if (builder.isType(HttpSession.class)) {
            return builder.location(Location.SYSTEM).convertable(false).parser(this::getSession);
        } else if (builder.isAssignableTo(Principal.class)) {
            return builder.location(Location.SYSTEM).convertable(false).parser((req, ctx) -> getPrincipal(ctx, builder.actualClass()));
        } else if (builder.isType(HttpMethod.class)) {
            return builder.location(Location.SYSTEM).convertable(false).parser(this::getHttpMethod);
        } else if (builder.isType(Locale.class)) {
            return builder.location(Location.SYSTEM).convertable(false).parser(this::getLocale);
        }
        return builder;
    }

    /**
     * Retrieves the WebRequest from the request context
     *
     * @param request The MCP request
     * @param ctx     The MCP request context
     * @return The WebRequest instance
     */
    private WebRequest getWebRequest(McpRequest request, McpRequestContext ctx) {
        return ((JakartaRequestContext) ctx).getWebRequest();
    }

    /**
     * Retrieves the HttpServletRequest from the context
     *
     * @param ctx The MCP request context
     * @return The HttpServletRequest instance
     */
    private HttpServletRequest getHttpRequest(McpRequestContext ctx) {
        return ((JakartaRequestContext) ctx).getHttpRequest();
    }

    /**
     * Retrieves the HttpServletRequest from the request and context
     *
     * @param request The MCP request
     * @param ctx The MCP request context
     * @return The HttpServletRequest instance
     */
    private HttpServletRequest getHttpRequest(McpRequest request, McpRequestContext ctx) {
        return getHttpRequest(ctx);
    }

    /**
     * Retrieves the HttpServletResponse from the request and context
     *
     * @param request The MCP request
     * @param ctx The MCP request context
     * @return The HttpServletResponse instance
     */
    private HttpServletResponse getHttpResponse(McpRequest request, McpRequestContext ctx) {
        return ((JakartaRequestContext) ctx).getHttpResponse();
    }

    /**
     * Retrieves the HttpSession from the context
     *
     * @param ctx The MCP request context
     * @return The HttpSession instance
     */
    private HttpSession getSession(McpRequestContext ctx) {
        return getHttpRequest(ctx).getSession();
    }

    /**
     * Retrieves the HttpSession from the request and context
     *
     * @param request The MCP request
     * @param ctx The MCP request context
     * @return The HttpSession instance
     */
    private HttpSession getSession(McpRequest request, McpRequestContext ctx) {
        return getSession(ctx);
    }

    /**
     * Retrieves the Principal from the context, ensuring it matches the expected type
     *
     * @param ctx The MCP request context
     * @param type The expected Principal type
     * @return The Principal instance or null if not found or not matching the expected type
     */
    private Principal getPrincipal(McpRequestContext ctx, Class<?> type) {
        Principal principal = getHttpRequest(ctx).getUserPrincipal();
        return !type.isInstance(principal) ? null : principal;
    }

    /**
     * Retrieves the Locale from the request
     *
     * @param request The MCP request
     * @param ctx The MCP request context
     * @return The Locale
     */
    private Locale getLocale(McpRequest request, McpRequestContext ctx) {
        return getHttpRequest(ctx).getLocale();
    }

    /**
     * Retrieves the HTTP method from the request
     *
     * @param request The MCP request
     * @param ctx The MCP request context
     * @return The HTTP method as a string
     */
    private Object getHttpMethod(McpRequest request, McpRequestContext ctx) {
        return getHttpRequest(ctx).getMethod();
    }

}

