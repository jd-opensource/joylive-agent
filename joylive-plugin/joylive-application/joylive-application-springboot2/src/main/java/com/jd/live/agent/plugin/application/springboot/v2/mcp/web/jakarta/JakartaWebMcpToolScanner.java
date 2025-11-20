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
package com.jd.live.agent.plugin.application.springboot.v2.mcp.web.jakarta;

import com.jd.live.agent.governance.mcp.McpRequest;
import com.jd.live.agent.governance.mcp.McpRequestContext;
import com.jd.live.agent.governance.mcp.McpToolParameter.Location;
import com.jd.live.agent.governance.mcp.McpToolParameter.McpToolParameterBuilder;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.AbstractMcpToolScanner;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.converter.OptionalConverter;
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
 * Default scanner for MCP tools based on Spring MVC controllers.
 */
public class JakartaWebMcpToolScanner extends AbstractMcpToolScanner {

    public JakartaWebMcpToolScanner(ConfigurableListableBeanFactory beanFactory) {
        super(beanFactory);
    }

    @Override
    protected McpToolParameterBuilder configureType(McpToolParameterBuilder builder) {
        // convert optional
        if (builder.type() == Optional.class) {
            Type actualType = getActualType(builder.genericType());
            if (actualType != null) {
                return builder.actualType(actualType).converter(OptionalConverter.INSTANCE);
            }
        }
        return builder;
    }

    @Override
    protected McpToolParameterBuilder configureSystemParam(McpToolParameterBuilder builder) {
        if (builder.isAssignableTo(WebRequest.class)) {
            return builder.location(Location.SYSTEM).systemParser(this::getWebRequest);
        } else if (builder.isType(HttpServletRequest.class)) {
            return builder.location(Location.SYSTEM).systemParser(this::getHttpRequest);
        } else if (builder.isType(HttpServletResponse.class)) {
            return builder.location(Location.SYSTEM).systemParser(this::getHttpResponse);
        } else if (builder.isType(HttpSession.class)) {
            return builder.location(Location.SYSTEM).systemParser(this::getSession);
        } else if (builder.isAssignableTo(Principal.class)) {
            return builder.location(Location.SYSTEM).systemParser((req, ctx) -> getPrincipal(ctx, builder.actualClass()));
        } else if (builder.isType(HttpMethod.class)) {
            return builder.location(Location.SYSTEM).systemParser(this::getHttpMethod);
        } else if (builder.isType(Locale.class)) {
            return builder.location(Location.SYSTEM).systemParser(this::getLocale);
        }
        return builder;
    }

    private WebRequest getWebRequest(McpRequest request, McpRequestContext ctx) {
        return ((JakartaRequestContext) ctx).getWebRequest();
    }

    private HttpServletRequest getHttpRequest(McpRequestContext ctx) {
        return ((JakartaRequestContext) ctx).getHttpRequest();
    }

    private HttpServletRequest getHttpRequest(McpRequest request, McpRequestContext ctx) {
        return getHttpRequest(ctx);
    }

    private HttpServletResponse getHttpResponse(McpRequest request, McpRequestContext ctx) {
        return ((JakartaRequestContext) ctx).getHttpResponse();
    }

    private HttpSession getSession(McpRequestContext ctx) {
        return getHttpRequest(ctx).getSession();
    }

    private HttpSession getSession(McpRequest request, McpRequestContext ctx) {
        return getSession(ctx);
    }

    private Principal getPrincipal(McpRequestContext ctx, Class<?> type) {
        Principal principal = getHttpRequest(ctx).getUserPrincipal();
        return !type.isInstance(principal) ? null : principal;
    }

    private Locale getLocale(McpRequest request, McpRequestContext ctx) {
        return getHttpRequest(ctx).getLocale();
    }

    private Object getHttpMethod(McpRequest request, McpRequestContext ctx) {
        return getHttpRequest(ctx).getMethod();
    }

}

