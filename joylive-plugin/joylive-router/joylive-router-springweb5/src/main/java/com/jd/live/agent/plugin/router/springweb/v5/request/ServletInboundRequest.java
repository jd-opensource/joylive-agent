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
package com.jd.live.agent.plugin.router.springweb.v5.request;

import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpInboundRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;
import static com.jd.live.agent.plugin.router.springweb.v5.exception.SpringInboundThrower.THROWER;

/**
 * ServletHttpInboundRequest
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class ServletInboundRequest extends AbstractHttpInboundRequest<HttpServletRequest> {

    private static final String RESOURCE_HANDLER_TYPE = "org.springframework.web.servlet.resource.ResourceHttpRequestHandler";

    private static final String ERROR_CONTROLLER_TYPE = "org.springframework.boot.web.servlet.error.ErrorController";

    private static final String ACTUATOR_SERVLET_TYPE = "org.springframework.boot.actuate.endpoint.web.servlet.AbstractWebMvcEndpointHandlerMapping$WebMvcEndpointHandlerMethod";

    private static final Class<?> ERROR_CONTROLLER_CLASS = loadClass(ERROR_CONTROLLER_TYPE, HttpServletRequest.class.getClassLoader());

    private static final Class<?> RESOURCE_HANDLER_CLASS = loadClass(RESOURCE_HANDLER_TYPE, HttpServletRequest.class.getClassLoader());

    private static final Class<?> ACTUATOR_SERVLET_CLASS = loadClass(ACTUATOR_SERVLET_TYPE, HttpServletRequest.class.getClassLoader());

    private final Object handler;

    private final Predicate<String> systemPredicate;

    public ServletInboundRequest(HttpServletRequest request, Object handler, Predicate<String> systemPredicate) {
        super(request);
        this.handler = handler;
        this.systemPredicate = systemPredicate;
        URI u = null;
        try {
            u = new URI(request.getRequestURI());
        } catch (URISyntaxException ignore) {
        }
        uri = u;
        headers = new UnsafeLazyObject<>(() -> HttpUtils.parseHeader(request.getHeaderNames(), request::getHeaders));
        queries = new UnsafeLazyObject<>(() -> HttpUtils.parseQuery(request.getQueryString()));
        cookies = new UnsafeLazyObject<>(() -> parseCookie(request));
    }

    @Override
    public HttpMethod getHttpMethod() {
        try {
            return HttpMethod.valueOf(request.getMethod());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String getClientIp() {
        String result = super.getClientIp();
        return result != null && !result.isEmpty() ? result : request.getRemoteAddr();
    }

    @Override
    public boolean isSystem() {
        if (handler != null) {
            if (RESOURCE_HANDLER_CLASS != null && RESOURCE_HANDLER_CLASS.isInstance(handler)) {
                return true;
            } else if (handler instanceof HandlerMethod
                    && ERROR_CONTROLLER_CLASS != null
                    && ERROR_CONTROLLER_CLASS.isInstance(((HandlerMethod) handler).getBean())) {
                return true;
            } else if (ACTUATOR_SERVLET_CLASS != null && ACTUATOR_SERVLET_CLASS.isInstance(handler)) {
                return true;
            }
        }
        if (systemPredicate != null && systemPredicate.test(getPath())) {
            return true;
        }
        return super.isSystem();
    }

    @Override
    protected String parseScheme() {
        String result = super.parseScheme();
        return result == null ? request.getScheme() : result;
    }

    @Override
    protected int parsePort() {
        int result = super.parsePort();
        return result >= 0 ? result : request.getServerPort();
    }

    @Override
    protected String parseHost() {
        String result = uri.getHost();
        if (result == null || !isDomain(result)) {
            String candidate = parseHostByHeader();
            if (candidate != null && isDomain(candidate)) {
                result = candidate;
            } else {
                candidate = request.getServerName();
                if (candidate != null && isDomain(candidate)) {
                    result = candidate;
                }
            }
        }
        return result;
    }

    /**
     * Converts an object to a ModelAndView.
     * <p>
     * This method checks if the object is already a ModelAndView, and if so, returns it directly.
     * Otherwise, it returns null.
     * </p>
     *
     * @param obj the object to convert to a ModelAndView.
     * @return a ModelAndView representing the object, or null if the object is not a ModelAndView.
     */
    public ModelAndView convert(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof ModelAndView) {
            return (ModelAndView) obj;
        } else if (obj instanceof Throwable) {
            return new ExceptionView(THROWER.createException((Throwable) obj, this));
        } else {
            return new ExceptionView(THROWER.createException(new UnsupportedOperationException(
                    "Expected type is " + ModelAndView.class.getName() + ", but actual type is " + obj.getClass()), this));
        }
    }

    private Map<String, List<String>> parseCookie(HttpServletRequest request) {
        Map<String, List<String>> result = new HashMap<>();
        if (request.getCookies() != null) {
            for (javax.servlet.http.Cookie cookie : request.getCookies()) {
                result.computeIfAbsent(cookie.getName(), name -> new ArrayList<>()).add(cookie.getValue());
            }
        }
        return result;
    }

}
