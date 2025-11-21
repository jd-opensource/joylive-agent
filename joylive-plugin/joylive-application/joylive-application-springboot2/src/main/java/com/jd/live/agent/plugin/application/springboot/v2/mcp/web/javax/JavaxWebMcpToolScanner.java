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

import com.jd.live.agent.core.mcp.McpRequest;
import com.jd.live.agent.core.mcp.McpRequestContext;
import com.jd.live.agent.core.mcp.McpToolParameter.Location;
import com.jd.live.agent.core.mcp.McpToolParameter.McpToolParameterBuilder;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.AbstractMcpToolScanner;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.converter.OptionalConverter;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Type;
import java.security.Principal;
import java.util.Locale;
import java.util.Optional;

/**
 * MCP tool scanner implementation for Javax Servlet API, used to scan MCP tools based on Spring MVC controllers.
 * Handles request context and parameter resolution related to Javax Servlet API.
 */
public class JavaxWebMcpToolScanner extends AbstractMcpToolScanner {

    /**
     * Creates a new JavaxWebMcpToolScanner instance
     *
     * @param beanFactory Spring Bean factory for dependency resolution
     */
    public JavaxWebMcpToolScanner(ConfigurableListableBeanFactory beanFactory) {
        super(beanFactory);
    }

    /**
     * Configures parameter wrapper, handling Optional type parameters
     *
     * @param builder Parameter builder
     * @return Configured parameter builder
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
     * Configures system parameters, marking Spring Web and Javax Servlet API related parameters as system parameters
     * and setting appropriate parsers
     *
     * @param builder Parameter builder
     * @return Configured parameter builder
     */
    @Override
    protected McpToolParameterBuilder configureSystemParam(McpToolParameterBuilder builder) {
        if (builder.isAssignableTo(WebRequest.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser(this::getWebRequest);
        } else if (builder.isType(HttpServletRequest.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser(this::getHttpRequest);
        } else if (builder.isType(HttpServletResponse.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser(this::getHttpResponse);
        } else if (builder.isType(HttpSession.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser(this::getSession);
        } else if (builder.isAssignableTo(Principal.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser((req, ctx) -> getPrincipal(ctx, builder.actualClass()));
        } else if (builder.isType(HttpMethod.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser(this::getHttpMethod);
        } else if (builder.isType(Locale.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser(this::getLocale);
        }
        return builder;
    }

    /**
     * Gets WebRequest object from request context
     *
     * @param ctx Request context
     * @return WebRequest object
     */
    private WebRequest getWebRequest(McpRequestContext ctx) {
        return ((JavaxRequestContext) ctx).getWebRequest();
    }

    /**
     * Gets WebRequest object from request and context
     *
     * @param request MCP request
     * @param ctx     Request context
     * @return WebRequest object
     */
    private WebRequest getWebRequest(McpRequest request, McpRequestContext ctx) {
        return getWebRequest(ctx);
    }

    /**
     * Gets HttpServletRequest object from request context
     *
     * @param ctx Request context
     * @return HttpServletRequest object
     */
    private HttpServletRequest getHttpRequest(McpRequestContext ctx) {
        return ((JavaxRequestContext) ctx).getHttpRequest();
    }

    /**
     * Gets HttpServletRequest object from request and context
     *
     * @param request MCP request
     * @param ctx Request context
     * @return HttpServletRequest object
     */
    private HttpServletRequest getHttpRequest(McpRequest request, McpRequestContext ctx) {
        return getHttpRequest(ctx);
    }

    /**
     * Gets HttpServletResponse object from request and context
     *
     * @param request MCP request
     * @param ctx Request context
     * @return HttpServletResponse object
     */
    private HttpServletResponse getHttpResponse(McpRequest request, McpRequestContext ctx) {
        return ((JavaxRequestContext) ctx).getHttpResponse();
    }

    /**
     * Gets Principal object of specified type
     *
     * @param ctx Request context
     * @param type Target type of Principal
     * @return Principal if instance matches specified type, otherwise null
     */
    private Principal getPrincipal(McpRequestContext ctx, Class<?> type) {
        Principal principal = getHttpRequest(ctx).getUserPrincipal();
        return !type.isInstance(principal) ? null : principal;
    }

    /**
     * Gets HttpSession object from request context
     *
     * @param ctx Request context
     * @return HttpSession object
     */
    private HttpSession getSession(McpRequestContext ctx) {
        return getHttpRequest(ctx).getSession();
    }

    /**
     * Gets HttpSession object from request and context
     *
     * @param request MCP request
     * @param ctx Request context
     * @return HttpSession object
     */
    private HttpSession getSession(McpRequest request, McpRequestContext ctx) {
        return getSession(ctx);
    }

    /**
     * Gets Locale object from request
     *
     * @param request MCP request
     * @param ctx Request context
     * @return Locale object
     */
    private Locale getLocale(McpRequest request, McpRequestContext ctx) {
        return getHttpRequest(ctx).getLocale();
    }

    /**
     * Gets HTTP request method
     *
     * @param request MCP request
     * @param ctx Request context
     * @return HTTP method string
     */
    private Object getHttpMethod(McpRequest request, McpRequestContext ctx) {
        return getHttpRequest(ctx).getMethod();
    }

}

