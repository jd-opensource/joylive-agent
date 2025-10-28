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

import com.jd.live.agent.governance.mcp.ExpressionFactory;
import com.jd.live.agent.governance.mcp.McpToolParameter.McpToolParameterBuilder;
import com.jd.live.agent.governance.mcp.RequestContext;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.AbstractMcpToolScanner;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.converter.OptionalConverter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.security.Principal;
import java.util.*;
import java.util.function.BiConsumer;

import static com.jd.live.agent.core.util.StringUtils.choose;

/**
 * Default scanner for MCP tools based on Spring MVC controllers.
 */
public class JavaxWebMcpToolScanner extends AbstractMcpToolScanner {

    public JavaxWebMcpToolScanner(ExpressionFactory expressionFactory) {
        super(expressionFactory);
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
            return builder.parser(this::getWebRequest);
        } else if (builder.isType(HttpServletRequest.class)) {
            return builder.parser(this::getHttpRequest);
        } else if (builder.isType(HttpServletResponse.class)) {
            return builder.parser(this::getHttpResponse);
        } else if (builder.isType(HttpSession.class)) {
            return builder.parser(this::getSession);
        } else if (builder.isAssignableTo(Principal.class)) {
            return builder.parser(ctx -> getPrincipal(ctx, builder.actualClass()));
        } else if (builder.isType(HttpMethod.class)) {
            return builder.parser(this::getHttpMethod);
        } else if (builder.isType(Locale.class)) {
            return builder.parser(this::getLocale);
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
                return configureCookieValue(builder, cookieValue);
            }
            RequestHeader requestHeader = parameter.getAnnotation(RequestHeader.class);
            if (requestHeader != null) {
                return configureRequestHeader(builder, requestHeader, builder.actualClass());
            }
        }
        return builder;
    }

    private McpToolParameterBuilder configureRequestAttribute(McpToolParameterBuilder builder, RequestAttribute requestAttribute) {
        String arg = choose(requestAttribute.value(), requestAttribute.name());
        String name = choose(arg, builder.name());
        return builder.arg(arg).parser(ctx -> getRequestAttribute(ctx, name));
    }

    private WebRequest getWebRequest(RequestContext ctx) {
        return ((JavaxRequestContext) ctx).getWebRequest();
    }

    private HttpServletRequest getHttpRequest(RequestContext ctx) {
        return ((JavaxRequestContext) ctx).getHttpRequest();
    }

    private HttpServletResponse getHttpResponse(RequestContext ctx) {
        return ((JavaxRequestContext) ctx).getHttpResponse();
    }

    private Principal getPrincipal(RequestContext ctx, Class<?> type) {
        Principal principal = getHttpRequest(ctx).getUserPrincipal();
        return principal == null || !type.isInstance(principal) ? null : principal;
    }

    private HttpSession getSession(RequestContext ctx) {
        return getHttpRequest(ctx).getSession();
    }

    private Locale getLocale(RequestContext ctx) {
        return getHttpRequest(ctx).getLocale();
    }

    private Object getHttpMethod(RequestContext ctx) {
        return getHttpRequest(ctx).getMethod();
    }

    private McpToolParameterBuilder configureRequestHeader(McpToolParameterBuilder builder, RequestHeader requestHeader, Class<?> type) {
        String arg = choose(requestHeader.value(), requestHeader.name());
        String name = choose(arg, builder.name());
        return builder
                .convertable(true)
                .arg(arg)
                .parser(ctx -> getHeader(ctx, name, builder.actualClass()))
                .defaultValueParser(createDefaultValueParser(requestHeader.defaultValue()));
    }

    private McpToolParameterBuilder configureCookieValue(McpToolParameterBuilder builder, CookieValue cookieValue) {
        String arg = choose(cookieValue.value(), cookieValue.name());
        String name = choose(arg, builder.name());
        return builder
                .convertable(true)
                .arg(arg)
                .parser(ctx -> getCookieValue(ctx, name))
                .defaultValueParser(createDefaultValueParser(cookieValue.defaultValue()));
    }

    private McpToolParameterBuilder configureSessionAttribute(McpToolParameterBuilder builder, SessionAttribute sessionAttribute) {
        String arg = choose(sessionAttribute.value(), sessionAttribute.name());
        String name = choose(arg, builder.name());
        return builder.arg(arg).parser(ctx -> getSessionAttribute(ctx, name));
    }

    private Object getRequestAttribute(RequestContext ctx, String name) {
        return getHttpRequest(ctx).getAttribute(name);
    }

    private Object getSessionAttribute(RequestContext ctx, String name) {
        return getSession(ctx).getAttribute(name);
    }

    private String getCookieValue(RequestContext ctx, String name) {
        Cookie[] cookies = getHttpRequest(ctx).getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private Object getHeader(RequestContext ctx, String name, Class<?> type) {
        WebRequest webRequest = getWebRequest(ctx);
        if (HttpHeaders.class == type) {
            HttpHeaders headers = new HttpHeaders();
            processHeader(webRequest, (key, value) -> headers.add(key, value));
            return headers;
        } else if (MultiValueMap.class == type || LinkedMultiValueMap.class == type) {
            MultiValueMap<String, String> headers = new LinkedMultiValueMap();
            processHeader(webRequest, (key, value) -> headers.add(key, value));
            return headers;
        } else if (Map.class == type || HashMap.class == type || LinkedHashMap.class == type) {
            Map<String, String> headers = new LinkedHashMap();
            processHeader(webRequest, (key, value) -> headers.put(key, value));
            return headers;
        } else {
            String[] values = webRequest.getHeaderValues(name);
            if (values == null) {
                return null;
            }
            return values.length == 1 ? values[0] : values;
        }
    }

    private void processHeader(WebRequest webRequest, BiConsumer<String, String> consumer) {
        Iterator<String> headerNames = webRequest.getHeaderNames();
        while (headerNames.hasNext()) {
            String headerName = headerNames.next();
            String[] headerValues = webRequest.getHeaderValues(headerName);
            if (headerValues != null && headerValues.length > 0) {
                for (String headerValue : headerValues) {
                    consumer.accept(headerName, headerValue);
                }
            }
        }
    }

}

