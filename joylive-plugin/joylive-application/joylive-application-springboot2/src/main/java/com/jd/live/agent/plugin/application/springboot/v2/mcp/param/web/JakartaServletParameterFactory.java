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
package com.jd.live.agent.plugin.application.springboot.v2.mcp.param.web;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.governance.mcp.ParameterParser;
import com.jd.live.agent.governance.mcp.ParameterParser.DefaultParameterParser;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.param.SystemParameterFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.WebUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.Principal;
import java.util.Locale;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredMethod;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * Factory implementation for resolving javax.servlet parameters.
 * Extends ServletParameterFactory to handle servlet-specific parameter creation.
 */
public class JakartaServletParameterFactory extends ServletParameterFactory implements SystemParameterFactory {

    private static final String TYPE_JAKARTA_HTTP_SERVLET_REQUEST = "jakarta.servlet.http.HttpServletRequest";
    private static final Class<?> CLASS_JAKARTA_HTTP_SERVLET_REQUEST = loadClass(TYPE_JAKARTA_HTTP_SERVLET_REQUEST, ResourceLoader.class.getClassLoader());
    private static final String TYPE_JAKARTA_HTTP_SERVLET_RESPONSE = "jakarta.servlet.http.HttpServletResponse";
    private static final Class<?> CLASS_JAKARTA_HTTP_SERVLET_RESPONSE = loadClass(TYPE_JAKARTA_HTTP_SERVLET_RESPONSE, ResourceLoader.class.getClassLoader());
    private static final String TYPE_JAKARTA_HTTP_SESSION = "jakarta.servlet.http.HttpSession";
    private static final Class<?> CLASS_JAKARTA_HTTP_SESSION = loadClass(TYPE_JAKARTA_HTTP_SESSION, ResourceLoader.class.getClassLoader());
    private static final String TYPE_JAKARTA_COOKIE = "jakarta.servlet.http.Cookie";
    private static final Class<?> CLASS_JAKARTA_COOKIE = loadClass(TYPE_JAKARTA_COOKIE, ResourceLoader.class.getClassLoader());
    private static final FieldAccessor ACCESSOR_COOKE_VALUE = getAccessor(CLASS_JAKARTA_COOKIE, "value");
    private static final Method METHOD_GET_SESSION = getDeclaredMethod(CLASS_JAKARTA_HTTP_SERVLET_REQUEST, "getSession");
    private static final Method METHOD_GET_METHOD = getDeclaredMethod(CLASS_JAKARTA_HTTP_SERVLET_REQUEST, "getMethod");
    private static final Method METHOD_GET_ATTRIBUTE = getDeclaredMethod(CLASS_JAKARTA_HTTP_SERVLET_REQUEST, "getAttribute", new Class[]{String.class, int.class});
    private static final Method METHOD_GET_HEADER = getDeclaredMethod(CLASS_JAKARTA_HTTP_SERVLET_REQUEST, "getHeader", new Class[]{String.class});
    private static final Method METHOD_GET_USER_PRINCIPAL = getDeclaredMethod(CLASS_JAKARTA_HTTP_SERVLET_REQUEST, "getUserPrincipal");
    private static final Method METHOD_GET_LOCALE = getDeclaredMethod(CLASS_JAKARTA_HTTP_SERVLET_REQUEST, "getLocale");
    private static final Method METHOD_GET_COOKIE = CLASS_JAKARTA_HTTP_SERVLET_REQUEST == null ? null : getDeclaredMethod(WebUtils.class, "getCookie", new Class[]{CLASS_JAKARTA_HTTP_SERVLET_REQUEST, String.class});

    @Override
    public ParameterParser getParser(Parameter parameter) {
        if (CLASS_JAKARTA_HTTP_SERVLET_REQUEST == null) {
            // disabled
            return null;
        }
        Class<?> type = parameter.getType();
        if (WebRequest.class.isAssignableFrom(type)) {
            return new DefaultParameterParser(() -> getWebRequest(type));
        } else if (type == CLASS_JAKARTA_HTTP_SERVLET_REQUEST) {
            return new DefaultParameterParser(() -> getRequest());
        } else if (type == CLASS_JAKARTA_HTTP_SERVLET_RESPONSE) {
            return new DefaultParameterParser(() -> getResponse());
        } else if (type == CLASS_JAKARTA_HTTP_SESSION) {
            return new DefaultParameterParser(() -> getSession());
        } else if (Principal.class.isAssignableFrom(type)) {
            return new DefaultParameterParser(() -> getPrincipal(type));
        } else if (HttpMethod.class == type) {
            return new DefaultParameterParser(() -> getHttpMethod());
        } else if (Locale.class == type) {
            return new DefaultParameterParser(() -> getLocale());
        } else {
            RequestAttribute requestAttribute = parameter.getAnnotation(RequestAttribute.class);
            if (requestAttribute != null) {
                String name = requestAttribute.value().isEmpty() ? requestAttribute.name() : requestAttribute.value();
                return new DefaultParameterParser(() -> getAttribute(name, 0, type));
            }
            SessionAttribute sessionAttribute = parameter.getAnnotation(SessionAttribute.class);
            if (sessionAttribute != null) {
                String name = sessionAttribute.value().isEmpty() ? sessionAttribute.name() : requestAttribute.value();
                return new DefaultParameterParser(() -> getAttribute(name, 1, type));
            }
            CookieValue cookieValue = parameter.getAnnotation(CookieValue.class);
            if (cookieValue != null) {
                String name = cookieValue.value().isEmpty() ? cookieValue.name() : cookieValue.value();
                return new DefaultParameterParser(false, true, () -> getCookieValue(name));
            }
            RequestHeader requestHeader = parameter.getAnnotation(RequestHeader.class);
            if (requestHeader != null) {
                String name = requestHeader.value().isEmpty() ? requestHeader.name() : requestHeader.value();
                return new DefaultParameterParser(false, true, () -> getHeader(name));
            }
        }
        return null;
    }

    private Locale getLocale() {
        if (METHOD_GET_LOCALE == null) {
            return null;
        }
        try {
            Object request = getRequest();
            Object result = request == null ? null : METHOD_GET_LOCALE.invoke(request);
            return result instanceof Locale ? (Locale) result : null;
        } catch (Throwable e) {
            return null;
        }
    }

    private Object getHttpMethod() {
        if (METHOD_GET_METHOD == null) {
            return null;
        }
        try {
            Object request = getRequest();
            String method = request == null ? null : (String) METHOD_GET_METHOD.invoke(request);
            return getHttpMethod(method);
        } catch (Throwable e) {
            return null;
        }
    }

    private Object getRequest() {
        return getWebRequest(CLASS_JAKARTA_HTTP_SERVLET_REQUEST);
    }

    private Object getResponse() {
        return getWebResponse(CLASS_JAKARTA_HTTP_SERVLET_RESPONSE);
    }

    private Object getSession() {
        if (METHOD_GET_SESSION == null) {
            return null;
        }
        Object request = getRequest();
        try {
            return request == null ? null : METHOD_GET_SESSION.invoke(request);
        } catch (Throwable e) {
            return null;
        }
    }

    private Object getAttribute(String name, int scope, Class<?> type) {
        if (METHOD_GET_ATTRIBUTE == null) {
            return null;
        }
        try {
            Object request = getRequest();
            Object result = request == null ? null : METHOD_GET_ATTRIBUTE.invoke(request, name, scope);
            return result == null || !type.isInstance(result) ? null : result;
        } catch (Throwable e) {
            return null;
        }
    }

    private Object getPrincipal(Class<?> type) {
        if (METHOD_GET_USER_PRINCIPAL == null) {
            return null;
        }
        try {
            Object request = getRequest();
            Object result = request == null ? null : METHOD_GET_USER_PRINCIPAL.invoke(request);
            return result == null || !type.isInstance(result) ? null : result;
        } catch (Throwable e) {
            return null;
        }
    }

    private Object getCookieValue(String name) {
        if (METHOD_GET_COOKIE == null || ACCESSOR_COOKE_VALUE == null) {
            return null;
        }
        try {
            Object request = getRequest();
            Object cookie = request == null ? null : METHOD_GET_COOKIE.invoke(null, request, name);
            return cookie == null ? null : ACCESSOR_COOKE_VALUE.get(cookie);
        } catch (Throwable e) {
            return null;
        }
    }

    private Object getHeader(String name) {
        if (METHOD_GET_COOKIE == null || ACCESSOR_COOKE_VALUE == null) {
            return null;
        }
        try {
            Object request = getRequest();
            return request == null ? null : METHOD_GET_HEADER.invoke(request, name);
        } catch (Throwable e) {
            return null;
        }
    }
}
