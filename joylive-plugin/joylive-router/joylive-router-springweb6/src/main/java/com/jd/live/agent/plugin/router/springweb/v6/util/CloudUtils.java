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
package com.jd.live.agent.plugin.router.springweb.v6.util;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * Utility class for detecting Spring Cloud environment and load balancer configuration.
 */
public class CloudUtils {

    private static final String TYPE_WEB_CLIENT_RESPONSE_EXCEPTION = "org.springframework.web.reactive.function.client.WebClientResponseException";

    private static final ClassLoader CLASS_LOADER = HttpHeaders.class.getClassLoader();

    private static final Class<?> CLASS_WEB_CLIENT_RESPONSE_EXCEPTION = loadClass(TYPE_WEB_CLIENT_RESPONSE_EXCEPTION, CLASS_LOADER);

    private static final String TYPE_HANDLER_METHOD = "org.springframework.web.method.HandlerMethod";

    private static final Class<?> CLASS_HANDLER_METHOD = loadClass(TYPE_HANDLER_METHOD, CLASS_LOADER);

    private static final FieldAccessor ACCESSOR_HANDLER = getAccessor(CLASS_HANDLER_METHOD, "bean");

    private static final String CONTROLLER_TYPE = "org.springframework.web.servlet.mvc.Controller";
    private static final Class<?> CONTROLLER_CLASS = loadClass(CONTROLLER_TYPE, CLASS_LOADER);
    private static final String ERROR_CONTROLLER_TYPE = "org.springframework.boot.web.servlet.error.ErrorController";
    private static final Class<?> ERROR_CONTROLLER_CLASS = loadClass(ERROR_CONTROLLER_TYPE, CLASS_LOADER);
    private static final String RESOURCE_HANDLER_TYPE = "org.springframework.web.servlet.resource.ResourceHttpRequestHandler";
    private static final Class<?> RESOURCE_HANDLER_CLASS = loadClass(RESOURCE_HANDLER_TYPE, CLASS_LOADER);
    private static final String ACTUATOR_SERVLET_TYPE = "org.springframework.boot.actuate.endpoint.web.servlet.AbstractWebMvcEndpointHandlerMapping$WebMvcEndpointHandlerMethod";
    private static final Class<?> ACTUATOR_SERVLET_CLASS = loadClass(ACTUATOR_SERVLET_TYPE, CLASS_LOADER);
    private static final String API_RESOURCE_CONTROLLER_TYPE = "springfox.documentation.swagger.web.ApiResourceController";
    private static final Class<?> API_RESOURCE_CONTROLLER_CLASS = loadClass(API_RESOURCE_CONTROLLER_TYPE, CLASS_LOADER);
    private static final String SWAGGER2_CONTROLLER_WEB_MVC_TYPE = "springfox.documentation.swagger2.web.Swagger2ControllerWebMvc";
    private static final Class<?> SWAGGER2_CONTROLLER_WEB_MVC_CLASS = loadClass(SWAGGER2_CONTROLLER_WEB_MVC_TYPE, CLASS_LOADER);
    private static final String OPEN_API_RESOURCE_TYPE = "org.springdoc.webmvc.api.OpenApiResource";
    private static final Class<?> OPEN_API_RESOURCE_CLASS = loadClass(OPEN_API_RESOURCE_TYPE, CLASS_LOADER);
    private static final String MULTIPLE_OPEN_API_RESOURCE_TYPE = "org.springdoc.webmvc.api.MultipleOpenApiResource";
    private static final Class<?> MULTIPLE_OPEN_API_RESOURCE_CLASS = loadClass(MULTIPLE_OPEN_API_RESOURCE_TYPE, CLASS_LOADER);
    private static final String SWAGGER_CONFIG_RESOURCE_TYPE = "org.springdoc.webmvc.ui.SwaggerConfigResource";
    private static final Class<?> SWAGGER_CONFIG_RESOURCE_CLASS = loadClass(SWAGGER_CONFIG_RESOURCE_TYPE, CLASS_LOADER);
    private static final String SWAGGER_UI_HOME_TYPE = "org.springdoc.webmvc.ui.SwaggerUiHome";
    private static final Class<?> SWAGGER_UI_HOME_CLASS = loadClass(SWAGGER_UI_HOME_TYPE, CLASS_LOADER);
    private static final String SWAGGER_WELCOME_COMMON_TYPE = "org.springdoc.webmvc.ui.SwaggerWelcomeCommon";
    private static final Class<?> SWAGGER_WELCOME_COMMON_CLASS = loadClass(SWAGGER_WELCOME_COMMON_TYPE, CLASS_LOADER);
    private static final String ACTUATOR_TYPE = "org.springframework.boot.actuate.endpoint.web.reactive.AbstractWebFluxEndpointHandlerMapping$WebFluxEndpointHandlerMethod";
    private static final Class<?> ACTUATOR_CLASS = loadClass(ACTUATOR_TYPE, CLASS_LOADER);
    private static final String RESOURCE_WEB_HANDLER_TYPE = "org.springframework.web.reactive.resource.ResourceWebHandler";
    private static final Class<?> RESOURCE_WEB_HANDLER_CLASS = loadClass(RESOURCE_WEB_HANDLER_TYPE, CLASS_LOADER);
    private static final String SWAGGER2_CONTROLLER_WEBFLUX_TYPE = "springfox.documentation.swagger2.web.Swagger2ControllerWebFlux";
    private static final Class<?> SWAGGER2_CONTROLLER_WEBFLUX_CLASS = loadClass(SWAGGER2_CONTROLLER_WEBFLUX_TYPE, CLASS_LOADER);

    private static final List<Class<?>> SYSTEM_HANDLERS = Arrays.asList(
            CONTROLLER_CLASS,
            RESOURCE_HANDLER_CLASS,
            RESOURCE_WEB_HANDLER_CLASS,
            ERROR_CONTROLLER_CLASS,
            API_RESOURCE_CONTROLLER_CLASS,
            ACTUATOR_CLASS,
            OPEN_API_RESOURCE_CLASS,
            MULTIPLE_OPEN_API_RESOURCE_CLASS,
            SWAGGER_CONFIG_RESOURCE_CLASS,
            SWAGGER_UI_HOME_CLASS,
            SWAGGER_WELCOME_COMMON_CLASS,
            SWAGGER2_CONTROLLER_WEB_MVC_CLASS,
            SWAGGER2_CONTROLLER_WEBFLUX_CLASS,
            ACTUATOR_SERVLET_CLASS
    ).stream().filter(v -> v != null).collect(Collectors.toList());


    /**
     * Creates writable copy of HTTP headers.
     *
     * @param headers source headers
     * @return writable headers instance
     */
    public static HttpHeaders writable(HttpHeaders headers) {
        return HttpHeaders.writableHttpHeaders(headers);
    }

    public static Object getHandler(Object handlerMethod) {
        return handlerMethod != null && CLASS_HANDLER_METHOD.isInstance(handlerMethod) ? ACCESSOR_HANDLER.get(handlerMethod) : null;
    }

    public static boolean isSystemHandler(Object handler) {
        if (handler != null) {
            for (Class<?> clazz : SYSTEM_HANDLERS) {
                if (clazz.isInstance(handler)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Safely extracts error message from exception, including WebClient response body when available.
     *
     * @param e the exception to process
     * @return the response body for WebClient exceptions, or regular message otherwise
     */
    public static String getErrorMessage(Throwable e) {
        // without webflux
        if (CLASS_WEB_CLIENT_RESPONSE_EXCEPTION != null && CLASS_WEB_CLIENT_RESPONSE_EXCEPTION.isInstance(e)) {
            WebClientResponseException webError = (WebClientResponseException) e;
            return webError.getResponseBodyAsString();
        }
        return e.getMessage();
    }
}
