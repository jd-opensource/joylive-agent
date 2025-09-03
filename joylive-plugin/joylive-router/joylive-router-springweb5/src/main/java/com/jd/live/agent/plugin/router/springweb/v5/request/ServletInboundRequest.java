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

import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpInboundRequest;
import com.jd.live.agent.governance.request.HeaderProvider;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
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

    private static final String ERROR_CONTROLLER_TYPE = "org.springframework.boot.web.servlet.error.ErrorController";

    private static final Class<?> ERROR_CONTROLLER_CLASS = loadClass(ERROR_CONTROLLER_TYPE, HttpServletRequest.class.getClassLoader());

    private static final String RESOURCE_HANDLER_TYPE = "org.springframework.web.servlet.resource.ResourceHttpRequestHandler";

    private static final Class<?> RESOURCE_HANDLER_CLASS = loadClass(RESOURCE_HANDLER_TYPE, HttpServletRequest.class.getClassLoader());

    private static final String ACTUATOR_SERVLET_TYPE = "org.springframework.boot.actuate.endpoint.web.servlet.AbstractWebMvcEndpointHandlerMapping$WebMvcEndpointHandlerMethod";

    private static final Class<?> ACTUATOR_SERVLET_CLASS = loadClass(ACTUATOR_SERVLET_TYPE, HttpServletRequest.class.getClassLoader());

    private static final String API_RESOURCE_CONTROLLER_TYPE = "springfox.documentation.swagger.web.ApiResourceController";

    private static final Class<?> API_RESOURCE_CONTROLLER_CLASS = loadClass(API_RESOURCE_CONTROLLER_TYPE, HttpServletRequest.class.getClassLoader());

    private static final String SWAGGER2_CONTROLLER_WEB_MVC_TYPE = "springfox.documentation.swagger2.web.Swagger2ControllerWebMvc";

    private static final Class<?> SWAGGER2_CONTROLLER_WEB_MVC_CLASS = loadClass(SWAGGER2_CONTROLLER_WEB_MVC_TYPE, HttpServletRequest.class.getClassLoader());

    private static final String OPEN_API_RESOURCE_TYPE = "org.springdoc.webmvc.api.OpenApiResource";

    private static final Class<?> OPEN_API_RESOURCE_CLASS = loadClass(OPEN_API_RESOURCE_TYPE, HttpServletRequest.class.getClassLoader());

    private static final String MULTIPLE_OPEN_API_RESOURCE_TYPE = "org.springdoc.webmvc.api.MultipleOpenApiResource";

    private static final Class<?> MULTIPLE_OPEN_API_RESOURCE_CLASS = loadClass(MULTIPLE_OPEN_API_RESOURCE_TYPE, HttpServletRequest.class.getClassLoader());

    private static final String SWAGGER_CONFIG_RESOURCE_TYPE = "org.springdoc.webmvc.ui.SwaggerConfigResource";

    private static final Class<?> SWAGGER_CONFIG_RESOURCE_CLASS = loadClass(SWAGGER_CONFIG_RESOURCE_TYPE, HttpServletRequest.class.getClassLoader());

    private static final String SWAGGER_UI_HOME_TYPE = "org.springdoc.webmvc.ui.SwaggerUiHome";

    private static final Class<?> SWAGGER_UI_HOME_CLASS = loadClass(SWAGGER_UI_HOME_TYPE, HttpServletRequest.class.getClassLoader());

    private static final String SWAGGER_WELCOME_COMMON_TYPE = "org.springdoc.webmvc.ui.SwaggerWelcomeCommon";

    private static final Class<?> SWAGGER_WELCOME_COMMON_CLASS = loadClass(SWAGGER_WELCOME_COMMON_TYPE, HttpServletRequest.class.getClassLoader());

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
    }

    @Override
    public HttpMethod getHttpMethod() {
        return HttpMethod.ofNullable(request.getMethod());
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
            } else if (handler instanceof HandlerMethod) {
                HandlerMethod method = (HandlerMethod) handler;
                Object bean = method.getBean();
                if (ERROR_CONTROLLER_CLASS != null && ERROR_CONTROLLER_CLASS.isInstance(bean)
                        || API_RESOURCE_CONTROLLER_CLASS != null && API_RESOURCE_CONTROLLER_CLASS.isInstance(bean)
                        || SWAGGER2_CONTROLLER_WEB_MVC_CLASS != null && SWAGGER2_CONTROLLER_WEB_MVC_CLASS.isInstance(bean)
                        || SWAGGER_CONFIG_RESOURCE_CLASS != null && SWAGGER_CONFIG_RESOURCE_CLASS.isInstance(bean)
                        || OPEN_API_RESOURCE_CLASS != null && OPEN_API_RESOURCE_CLASS.isInstance(bean)
                        || MULTIPLE_OPEN_API_RESOURCE_CLASS != null && MULTIPLE_OPEN_API_RESOURCE_CLASS.isInstance(bean)
                        || SWAGGER_UI_HOME_CLASS != null && SWAGGER_UI_HOME_CLASS.isInstance(bean)
                        || SWAGGER_WELCOME_COMMON_CLASS != null && SWAGGER_WELCOME_COMMON_CLASS.isInstance(bean)) {
                    return true;
                }
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
    protected Address parseAddress() {
        Address result = super.parseAddress();
        if (result == null) {
            result = parseAddressByRequest();
        }
        return result;
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return request instanceof HeaderProvider
                ? ((HeaderProvider) request).getHeaders()
                : HttpUtils.parseHeader(request.getHeaderNames(), request::getHeaders);
    }

    @Override
    protected Map<String, List<String>> parseQueries() {
        return HttpUtils.parseQuery(request.getQueryString());
    }

    @Override
    protected Map<String, List<String>> parseCookies() {
        return HttpUtils.parseCookie(request.getCookies(), Cookie::getName, Cookie::getValue);
    }

    /**
     * Parses the address from the HTTP request.
     *
     * @return the parsed address, or null if the server name is invalid
     */
    protected Address parseAddressByRequest() {
        String serverName = request.getServerName();
        if (validateHost(serverName)) {
            int serverPort = request.getServerPort();
            return new Address(serverName, serverPort < 0 ? null : serverPort);
        }
        return null;
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
}
