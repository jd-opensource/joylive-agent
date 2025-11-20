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
package com.jd.live.agent.plugin.application.springboot.v2.mcp.reactive;

import com.jd.live.agent.governance.mcp.McpRequest;
import com.jd.live.agent.governance.mcp.McpRequestContext;
import com.jd.live.agent.governance.mcp.McpToolParameter.Location;
import com.jd.live.agent.governance.mcp.McpToolParameter.McpToolParameterBuilder;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.AbstractMcpToolScanner;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.converter.MonoConverter;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.converter.OptionalConverter;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.security.Principal;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

/**
 * Scanner for MCP tools that supports reactive programming with Spring WebFlux.
 * This implementation handles reactive types like Mono and provides system parameter
 * resolution for WebFlux-specific types.
 */
public class ReactiveMcpToolScanner extends AbstractMcpToolScanner {

    /**
     * Creates a new reactive MCP tool scanner
     *
     * @param beanFactory The Spring bean factory to use for bean resolution
     */
    public ReactiveMcpToolScanner(ConfigurableListableBeanFactory beanFactory) {
        super(beanFactory);
    }

    /**
     * Filters methods to determine if they are valid MCP tools.
     * Only methods with supported return types and parameter types are accepted.
     *
     * @param method The method to check
     * @return true if the method is a valid MCP tool, false otherwise
     */
    @Override
    protected boolean filter(Method method) {
        // return type
        if (!support(method.getReturnType())) {
            return false;
        }
        // parameter
        for (Parameter parameter : method.getParameters()) {
            if (!support(parameter.getType())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Configures parameter wrappers for reactive types.
     * Handles Optional and Mono types by extracting their actual type parameters
     * and applying appropriate converters.
     *
     * @param builder The parameter builder to configure
     * @return The configured parameter builder
     */
    @Override
    protected McpToolParameterBuilder configureWrapper(McpToolParameterBuilder builder) {
        // convert optional
        Class<?> type = builder.type();
        if (type == Optional.class) {
            Type actualType = getActualType(builder.genericType());
            if (actualType != null) {
                return builder.actualType(actualType).wrapper(OptionalConverter.INSTANCE);
            }
        } else if (type == Mono.class) {
            Type actualType = getActualType(builder.genericType());
            if (actualType != null) {
                return builder.actualType(actualType).wrapper(MonoConverter.INSTANCE);
            }
        }
        return builder;
    }

    /**
     * Configures system parameters for WebFlux-specific types.
     * Maps various Spring WebFlux types to their appropriate system parameter resolvers.
     *
     * @param builder The parameter builder to configure
     * @return The configured parameter builder
     */
    @Override
    protected McpToolParameterBuilder configureSystemParam(McpToolParameterBuilder builder) {
        if (builder.isType(ServerWebExchange.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser(this::getWebExchange);
        } else if (builder.isType(ServerHttpRequest.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser(this::getRequest);
        } else if (builder.isType(ServerHttpResponse.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser(this::getResponse);
        } else if (builder.isType(WebSession.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser(this::getSession);
        } else if (builder.isType(Principal.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser(this::getPrincipal);
        } else if (builder.isType(HttpMethod.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser(this::getHttpMethod);
        } else if (builder.isType(Locale.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser(this::getLocale);
        } else if (builder.isType(TimeZone.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser(this::getTimeZone);
        } else if (builder.isType(ZoneId.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser(this::getZoneId);
        } else if (builder.isType(UriComponentsBuilder.class)) {
            return builder.location(Location.SYSTEM).convertable(false).systemParser(this::getUriComponentsBuilder);
        }
        return builder;
    }

    /**
     * Determines if a given type is supported by this scanner.
     * Only Mono is supported among Publisher types.
     *
     * @param type The class type to check
     * @return true if the type is supported, false otherwise
     */
    private boolean support(Class<?> type) {
        // only mono
        return !Publisher.class.isAssignableFrom(type) || type == Mono.class;
    }

    /**
     * Retrieves the ServerWebExchange from the request context
     *
     * @param ctx The MCP request context
     * @return The ServerWebExchange instance
     */
    private ServerWebExchange getWebExchange(McpRequestContext ctx) {
        return ((ReactiveRequestContext) ctx).getExchange();
    }

    /**
     * Retrieves the ServerWebExchange from the request and context
     *
     * @param request The MCP request
     * @param ctx     The MCP request context
     * @return The ServerWebExchange instance
     */
    private ServerWebExchange getWebExchange(McpRequest request, McpRequestContext ctx) {
        return getWebExchange(ctx);
    }

    /**
     * Retrieves the ServerHttpRequest from the context
     *
     * @param ctx The MCP request context
     * @return The ServerHttpRequest instance
     */
    private ServerHttpRequest getRequest(McpRequestContext ctx) {
        return getWebExchange(ctx).getRequest();
    }

    /**
     * Retrieves the ServerHttpRequest from the request and context
     *
     * @param request The MCP request
     * @param ctx The MCP request context
     * @return The ServerHttpRequest instance
     */
    private ServerHttpRequest getRequest(McpRequest request, McpRequestContext ctx) {
        return getRequest(ctx);
    }

    /**
     * Retrieves the ServerHttpResponse from the request and context
     *
     * @param request The MCP request
     * @param ctx The MCP request context
     * @return The ServerHttpResponse instance
     */
    private ServerHttpResponse getResponse(McpRequest request, McpRequestContext ctx) {
        return getWebExchange(ctx).getResponse();
    }

    /**
     * Retrieves the WebSession from the context as a Mono
     *
     * @param ctx The MCP request context
     * @return A Mono containing the WebSession
     */
    private Mono<WebSession> getSession(McpRequestContext ctx) {
        return getWebExchange(ctx).getSession();
    }

    /**
     * Retrieves the WebSession from the request and context as a Mono
     *
     * @param request The MCP request
     * @param ctx The MCP request context
     * @return A Mono containing the WebSession
     */
    private Mono<WebSession> getSession(McpRequest request, McpRequestContext ctx) {
        return getSession(ctx);
    }

    /**
     * Retrieves an attribute from the request
     *
     * @param ctx The MCP request context
     * @param name The attribute name
     * @return The attribute value or null if not found
     */
    private Object getRequestAttribute(McpRequestContext ctx, String name) {
        return getWebExchange(ctx).getAttribute(name);
    }

    /**
     * Retrieves an attribute from the session
     * Note: This method blocks to obtain the session
     *
     * @param ctx The MCP request context
     * @param name The attribute name
     * @return The attribute value or null if not found
     */
    private Object getSessionAttribute(McpRequestContext ctx, String name) {
        WebSession session = getSession(ctx).block();
        return session == null ? null : session.getAttribute(name);
    }

    /**
     * Retrieves the HTTP method from the request
     *
     * @param request The MCP request
     * @param ctx The MCP request context
     * @return The HTTP method
     */
    private HttpMethod getHttpMethod(McpRequest request, McpRequestContext ctx) {
        return getRequest(ctx).getMethod();
    }

    /**
     * Retrieves the Principal from the request as a Mono
     *
     * @param request The MCP request
     * @param ctx The MCP request context
     * @return A Mono containing the Principal
     */
    private Mono<Principal> getPrincipal(McpRequest request, McpRequestContext ctx) {
        return getWebExchange(ctx).getPrincipal();
    }

    /**
     * Retrieves the Locale from the request
     *
     * @param request The MCP request
     * @param ctx The MCP request context
     * @return The Locale
     */
    private Locale getLocale(McpRequest request, McpRequestContext ctx) {
        return getWebExchange(ctx).getLocaleContext().getLocale();
    }

    /**
     * Retrieves the TimeZone from the request
     * Falls back to the default TimeZone if not available
     *
     * @param request The MCP request
     * @param ctx The MCP request context
     * @return The TimeZone
     */
    private TimeZone getTimeZone(McpRequest request, McpRequestContext ctx) {
        TimeZone timeZone = null;
        ServerWebExchange exchange = getWebExchange(ctx);
        if (exchange != null) {
            LocaleContext localeContext = exchange.getLocaleContext();
            if (localeContext instanceof TimeZoneAwareLocaleContext) {
                timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
            }
        }
        return timeZone != null ? timeZone : TimeZone.getDefault();
    }

    /**
     * Retrieves the ZoneId from the request
     * Falls back to the system default ZoneId if not available
     *
     * @param request The MCP request
     * @param ctx The MCP request context
     * @return The ZoneId
     */
    private ZoneId getZoneId(McpRequest request, McpRequestContext ctx) {
        TimeZone timeZone = getTimeZone(request, ctx);
        return timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
    }

    /**
     * Creates a UriComponentsBuilder from the request
     * The builder is initialized with the request URI, path, and empty query
     *
     * @param request The MCP request
     * @param ctx The MCP request context
     * @return The UriComponentsBuilder
     */
    private UriComponentsBuilder getUriComponentsBuilder(McpRequest request, McpRequestContext ctx) {
        ServerHttpRequest req = getRequest(ctx);
        return UriComponentsBuilder.fromUri(req.getURI()).replacePath(req.getPath().contextPath().value()).replaceQuery(null);
    }
}
