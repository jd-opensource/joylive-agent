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
package com.jd.live.agent.plugin.router.springweb.v5.util;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * Utility class for detecting Spring Cloud environment and load balancer configuration.
 */
public class CloudUtils {

    private static final ClassLoader CLASS_LOADER = HttpHeaders.class.getClassLoader();

    private static final String TYPE_WEB_CLIENT_RESPONSE_EXCEPTION = "org.springframework.web.reactive.function.client.WebClientResponseException";
    private static final Class<?> CLASS_WEB_CLIENT_RESPONSE_EXCEPTION = loadClass(TYPE_WEB_CLIENT_RESPONSE_EXCEPTION, CLASS_LOADER);

    private static final String TYPE_HANDLER_METHOD = "org.springframework.web.method.HandlerMethod";
    private static final Class<?> CLASS_HANDLER_METHOD = loadClass(TYPE_HANDLER_METHOD, CLASS_LOADER);
    private static final FieldAccessor ACCESSOR_HANDLER = getAccessor(CLASS_HANDLER_METHOD, "bean");

    private static final String TYPE_HANDLER_RESULT = "org.springframework.web.reactive.HandlerResult";
    private static final Class<?> CLASS_HANDLER_RESULT = loadClass(TYPE_HANDLER_RESULT, CLASS_LOADER);
    private static final FieldAccessor ACCESSOR_EXCEPTION_HANDLER = getAccessor(CLASS_HANDLER_RESULT, "exceptionHandler");

    private static final String TYPE_ROUTER_FUNCTION_BUILDER = "org.springframework.web.servlet.function.RouterFunctionBuilder";
    private static final Class<?> CLASS_ROUTER_FUNCTION_BUILDER = loadClass(TYPE_ROUTER_FUNCTION_BUILDER, CLASS_LOADER);
    private static final FieldAccessor ACCESSOR_ERROR_HANDLERS = getAccessor(CLASS_ROUTER_FUNCTION_BUILDER, "errorHandlers");
    private static final Map<Object, Object> ROUTER_FUNCTION_ERRORS = new ConcurrentHashMap<>();

    private static final String TYPE_HANDLER_FUNCTION = "org.springframework.web.servlet.function.HandlerFunction";
    private static final Class<?> CLASS_HANDLER_FUNCTION = loadClass(TYPE_HANDLER_FUNCTION, CLASS_LOADER);
    private static final String TYPE_RESOURCE_HANDLER_FUNCTION = "org.springframework.web.servlet.function.ResourceHandlerFunction";
    private static final Class<?> CLASS_RESOURCE_HANDLER_FUNCTION = loadClass(TYPE_RESOURCE_HANDLER_FUNCTION, CLASS_LOADER);
    private static final Map<Object, Boolean> RESOURCE_HANDLERS = new ConcurrentHashMap<>();

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
        return handlerMethod != null && CLASS_HANDLER_METHOD != null && CLASS_HANDLER_METHOD.isInstance(handlerMethod)
                ? ACCESSOR_HANDLER.get(handlerMethod)
                : handlerMethod;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getExceptionHandler(Object handlerResult) {
        return ACCESSOR_EXCEPTION_HANDLER != null && CLASS_HANDLER_RESULT != null && CLASS_HANDLER_RESULT.isInstance(handlerResult) ? (T) ACCESSOR_EXCEPTION_HANDLER.get(handlerResult) : null;
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

    /**
     * Associates an error function with a router function for error handling.
     *
     * @param routerFunction The router function to associate with an error handler
     * @param errorFunction  The error handling function to be associated
     */
    public static void putErrorFunction(Object routerFunction, Object errorFunction) {
        if (routerFunction != null && errorFunction != null) {
            ROUTER_FUNCTION_ERRORS.put(routerFunction, errorFunction);
        }
    }

    /**
     * Retrieves the error function associated with a router function.
     *
     * @param routerFunction The router function whose error handler is requested
     * @param <T>            The expected type of the error function
     * @return The associated error function or null if none exists
     */
    @SuppressWarnings("unchecked")
    public static <T> T getErrorFunction(Object routerFunction) {
        return routerFunction == null ? null : (T) ROUTER_FUNCTION_ERRORS.get(routerFunction);
    }

    /**
     * Retrieves error handlers associated with a builder object.
     *
     * @param builder The builder object containing error handlers
     * @param <T>     The expected type of the error handlers
     * @return The error handlers associated with the builder
     */
    @SuppressWarnings("unchecked")
    public static <T> T getErrorHandlers(Object builder) {
        return (T) ACCESSOR_ERROR_HANDLERS.get(builder);
    }

    /**
     * Determines if a handler function is a resource handler function.
     * Uses reflection to inspect the handler's internal structure.
     *
     * @param handler The handler function to check
     * @return true if the handler is a resource handler function, false otherwise
     */
    public static boolean isResourceHandlerFunction(Object handler) {
        if (CLASS_HANDLER_FUNCTION == null || CLASS_RESOURCE_HANDLER_FUNCTION == null || !CLASS_HANDLER_FUNCTION.isInstance(handler)) {
            return false;
        }
        return RESOURCE_HANDLERS.computeIfAbsent(handler, k -> {
            try {
                Field field = k.getClass().getDeclaredField("arg$2");
                field.setAccessible(true);
                Object arg2 = field.get(k);
                return CLASS_RESOURCE_HANDLER_FUNCTION.isInstance(arg2);
            } catch (Throwable e) {
                return false;
            }
        });
    }
}
