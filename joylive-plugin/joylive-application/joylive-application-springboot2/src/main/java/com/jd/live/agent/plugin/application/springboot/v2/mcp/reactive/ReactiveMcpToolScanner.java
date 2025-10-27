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

import com.jd.live.agent.governance.mcp.McpToolParameter.McpToolParameterBuilder;
import com.jd.live.agent.governance.mcp.McpToolScanner;
import com.jd.live.agent.governance.mcp.RequestContext;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.AbstractMcpToolScanner;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.converter.MonoConverter;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.converter.OptionalConverter;
import org.reactivestreams.Publisher;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.security.Principal;
import java.time.ZoneId;
import java.util.*;

import static com.jd.live.agent.core.util.StringUtils.choose;

/**
 * Default scanner for MCP tools based on Spring MVC controllers.
 */
public class ReactiveMcpToolScanner extends AbstractMcpToolScanner {

    public static final McpToolScanner INSTANCE = new ReactiveMcpToolScanner();

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
        Class<?> type = builder.actualType() instanceof Class<?> ? (Class<?>) builder.actualType() : builder.type();
        if (builder.isType(ServerWebExchange.class)) {
            return builder.parser(this::getWebExchange);
        } else if (builder.isType(ServerHttpRequest.class)) {
            return builder.parser(this::getRequest);
        } else if (builder.isType(ServerHttpResponse.class)) {
            return builder.parser(this::getResponse);
        } else if (builder.isType(WebSession.class)) {
            return builder.parser(this::getSession);
        } else if (builder.isType(HttpHeaders.class)) {
            return builder.parser(this::getHeaders);
        } else if (builder.isType(Principal.class)) {
            return builder.parser(this::getPrincipal);
        } else if (builder.isType(HttpMethod.class)) {
            return builder.parser(this::getHttpMethod);
        } else if (builder.isType(Locale.class)) {
            return builder.parser(this::getLocale);
        } else if (builder.isType(TimeZone.class)) {
            return builder.parser(this::getTimeZone);
        } else if (builder.isType(ZoneId.class)) {
            return builder.parser(this::getZoneId);
        } else if (builder.isType(UriComponentsBuilder.class)) {
            return builder.parser(this::getUriComponentsBuilder);
        } else {
            Parameter parameter = builder.parameter();
            RequestAttribute requestAttribute = parameter.getAnnotation(RequestAttribute.class);
            if (requestAttribute != null) {
                return configureRequestAttribute(builder, requestAttribute);
            }
            SessionAttribute sessionAttribute = parameter.getAnnotation(SessionAttribute.class);
            if (sessionAttribute != null) {
                return configureSessionAttribute(builder, sessionAttribute);
            }
            CookieValue cookieValue = parameter.getAnnotation(CookieValue.class);
            if (cookieValue != null) {
                return configureCookieValue(builder, cookieValue, type);
            }
            RequestHeader requestHeader = parameter.getAnnotation(RequestHeader.class);
            if (requestHeader != null) {
                return configureRequestHeader(builder, requestHeader, type);
            }
        }
        return builder;
    }

    private boolean support(Class<?> type) {
        if (Publisher.class.isAssignableFrom(type) && type != Mono.class) {
            // only mono
            return false;
        }
        return true;
    }

    private ServerWebExchange getWebExchange(RequestContext ctx) {
        return ((ReactiveParserContext) ctx).getExchange();
    }

    private ServerHttpRequest getRequest(RequestContext ctx) {
        return getWebExchange(ctx).getRequest();
    }

    private ServerHttpResponse getResponse(RequestContext ctx) {
        return getWebExchange(ctx).getResponse();
    }

    private HttpHeaders getHeaders(RequestContext ctx) {
        return getRequest(ctx).getHeaders();
    }

    private Mono<WebSession> getSession(RequestContext ctx) {
        return getWebExchange(ctx).getSession();
    }

    private Object getRequestAttribute(RequestContext ctx, String name) {
        return getWebExchange(ctx).getAttribute(name);
    }

    private Object getSessionAttribute(RequestContext ctx, String name) {
        Mono<WebSession> mono = getSession(ctx);
        if (mono == null) {
            return null;
        }
        WebSession session = mono.block();
        return session == null ? null : session.getAttribute(name);
    }

    private HttpMethod getHttpMethod(RequestContext ctx) {
        return getRequest(ctx).getMethod();
    }

    private Mono<Principal> getPrincipal(RequestContext ctx) {
        return getWebExchange(ctx).getPrincipal();
    }

    private Locale getLocale(RequestContext ctx) {
        return getWebExchange(ctx).getLocaleContext().getLocale();
    }

    private TimeZone getTimeZone(RequestContext ctx) {
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

    private ZoneId getZoneId(RequestContext ctx) {
        TimeZone timeZone = getTimeZone(ctx);
        return timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
    }

    private UriComponentsBuilder getUriComponentsBuilder(RequestContext ctx) {
        ServerHttpRequest request = getRequest(ctx);
        return request == null
                ? UriComponentsBuilder.newInstance()
                : UriComponentsBuilder.fromUri(request.getURI()).replacePath(request.getPath().contextPath().value()).replaceQuery(null);
    }

    private McpToolParameterBuilder configureRequestHeader(McpToolParameterBuilder builder, RequestHeader requestHeader, Class<?> type) {
        String arg = choose(requestHeader.value(), requestHeader.name());
        String name = choose(arg, builder.name());
        return builder.convertable(true).arg(arg).parser(ctx -> getHeader(ctx, name, type));
    }

    private McpToolParameterBuilder configureCookieValue(McpToolParameterBuilder builder, CookieValue cookieValue, Class<?> type) {
        String arg = choose(cookieValue.value(), cookieValue.name());
        String name = choose(arg, builder.name());
        return builder.convertable(true).arg(arg).parser(ctx -> getCookieValue(ctx, name, type));
    }

    private McpToolParameterBuilder configureSessionAttribute(McpToolParameterBuilder builder, SessionAttribute sessionAttribute) {
        String arg = choose(sessionAttribute.value(), sessionAttribute.name());
        String name = choose(arg, builder.name());
        return builder.arg(arg).parser(ctx -> getSessionAttribute(ctx, name));
    }

    private McpToolParameterBuilder configureRequestAttribute(McpToolParameterBuilder builder, RequestAttribute requestAttribute) {
        String arg = choose(requestAttribute.value(), requestAttribute.name());
        String name = choose(arg, builder.name());
        return builder.arg(arg).parser(ctx -> getRequestAttribute(ctx, name));
    }

    private Object getCookieValue(RequestContext ctx, String name, Class<?> type) {
        ServerHttpRequest request = getRequest(ctx);
        if (request != null) {
            HttpCookie cookie = request.getCookies().getFirst(name);
            if (cookie != null) {
                return HttpCookie.class.isAssignableFrom(type) ? cookie : cookie.getValue();
            }
        }
        return null;
    }

    private Object getHeader(RequestContext ctx, String name, Class<?> type) {
        HttpHeaders headers = getHeaders(ctx);
        if (headers != null) {
            if (Map.class.isAssignableFrom(type)) {
                return headers.toSingleValueMap();
            }
            List<String> values = headers.get(name);
            if (values != null && !values.isEmpty()) {
                return values.get(0);
            }
        }
        return null;
    }
}
