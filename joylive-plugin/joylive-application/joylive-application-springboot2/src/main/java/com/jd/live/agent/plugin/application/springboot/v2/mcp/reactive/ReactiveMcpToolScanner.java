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
 * Default scanner for MCP tools based on Spring MVC controllers.
 */
public class ReactiveMcpToolScanner extends AbstractMcpToolScanner {

    public ReactiveMcpToolScanner(ConfigurableListableBeanFactory beanFactory) {
        super(beanFactory);
    }

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

    @Override
    protected McpToolParameterBuilder configureType(McpToolParameterBuilder builder) {
        // convert optional
        Class<?> type = builder.type();
        if (type == Optional.class) {
            Type actualType = getActualType(builder.genericType());
            if (actualType != null) {
                return builder.actualType(actualType).converter(OptionalConverter.INSTANCE);
            }
        } else if (type == Mono.class) {
            Type actualType = getActualType(builder.genericType());
            if (actualType != null) {
                return builder.actualType(actualType).converter(MonoConverter.INSTANCE);
            }
        }
        return builder;
    }

    @Override
    protected McpToolParameterBuilder configureSystemParam(McpToolParameterBuilder builder) {
        if (builder.isType(ServerWebExchange.class)) {
            return builder.location(Location.SYSTEM).systemParser(this::getWebExchange);
        } else if (builder.isType(ServerHttpRequest.class)) {
            return builder.location(Location.SYSTEM).systemParser(this::getRequest);
        } else if (builder.isType(ServerHttpResponse.class)) {
            return builder.location(Location.SYSTEM).systemParser(this::getResponse);
        } else if (builder.isType(WebSession.class)) {
            return builder.location(Location.SYSTEM).systemParser(this::getSession);
        } else if (builder.isType(Principal.class)) {
            return builder.location(Location.SYSTEM).systemParser(this::getPrincipal);
        } else if (builder.isType(HttpMethod.class)) {
            return builder.location(Location.SYSTEM).systemParser(this::getHttpMethod);
        } else if (builder.isType(Locale.class)) {
            return builder.location(Location.SYSTEM).systemParser(this::getLocale);
        } else if (builder.isType(TimeZone.class)) {
            return builder.location(Location.SYSTEM).systemParser(this::getTimeZone);
        } else if (builder.isType(ZoneId.class)) {
            return builder.location(Location.SYSTEM).systemParser(this::getZoneId);
        } else if (builder.isType(UriComponentsBuilder.class)) {
            return builder.location(Location.SYSTEM).systemParser(this::getUriComponentsBuilder);
        }
        return builder;
    }

    private boolean support(Class<?> type) {
        // only mono
        return !Publisher.class.isAssignableFrom(type) || type == Mono.class;
    }

    private ServerWebExchange getWebExchange(McpRequestContext ctx) {
        return ((ReactiveRequestContext) ctx).getExchange();
    }

    private ServerWebExchange getWebExchange(McpRequest request, McpRequestContext ctx) {
        return getWebExchange(ctx);
    }

    private ServerHttpRequest getRequest(McpRequestContext ctx) {
        return getWebExchange(ctx).getRequest();
    }

    private ServerHttpRequest getRequest(McpRequest request, McpRequestContext ctx) {
        return getRequest(ctx);
    }

    private ServerHttpResponse getResponse(McpRequest request, McpRequestContext ctx) {
        return getWebExchange(ctx).getResponse();
    }

    private Mono<WebSession> getSession(McpRequestContext ctx) {
        return getWebExchange(ctx).getSession();
    }

    private Mono<WebSession> getSession(McpRequest request, McpRequestContext ctx) {
        return getSession(ctx);
    }

    private Object getRequestAttribute(McpRequestContext ctx, String name) {
        return getWebExchange(ctx).getAttribute(name);
    }

    private Object getSessionAttribute(McpRequestContext ctx, String name) {
        WebSession session = getSession(ctx).block();
        return session == null ? null : session.getAttribute(name);
    }

    private HttpMethod getHttpMethod(McpRequest request, McpRequestContext ctx) {
        return getRequest(ctx).getMethod();
    }

    private Mono<Principal> getPrincipal(McpRequest request, McpRequestContext ctx) {
        return getWebExchange(ctx).getPrincipal();
    }

    private Locale getLocale(McpRequest request, McpRequestContext ctx) {
        return getWebExchange(ctx).getLocaleContext().getLocale();
    }

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

    private ZoneId getZoneId(McpRequest request, McpRequestContext ctx) {
        TimeZone timeZone = getTimeZone(request, ctx);
        return timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
    }

    private UriComponentsBuilder getUriComponentsBuilder(McpRequest request, McpRequestContext ctx) {
        ServerHttpRequest req = getRequest(ctx);
        return UriComponentsBuilder.fromUri(req.getURI()).replacePath(req.getPath().contextPath().value()).replaceQuery(null);
    }
}
