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
import com.jd.live.agent.core.util.type.ClassUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
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
    private static final String TYPE_REACTIVE_ROUTER_FUNCTION_BUILDER = "org.springframework.web.reactive.function.server.RouterFunctionBuilder";
    private static final Class<?> CLASS_REACTIVE_ROUTER_FUNCTION_BUILDER = loadClass(TYPE_REACTIVE_ROUTER_FUNCTION_BUILDER, CLASS_LOADER);
    private static final FieldAccessor ACCESSOR_REACTIVE_ERROR_HANDLERS = getAccessor(CLASS_REACTIVE_ROUTER_FUNCTION_BUILDER, "errorHandlers");
    private static final Map<Object, Object> ROUTER_FUNCTION_ERRORS = new ConcurrentHashMap<>();

    private static final String TYPE_REACTIVE_FILTERED_ROUTER_FUNCTION = "org.springframework.web.reactive.function.server.RouterFunctions$FilteredRouterFunction";
    private static final Class<?> CLASS_REACTIVE_FILTERED_ROUTER_FUNCTION = loadClass(TYPE_REACTIVE_FILTERED_ROUTER_FUNCTION, CLASS_LOADER);
    private static final FieldAccessor ACCESSOR_REACTIVE_FILTER_FUNCTION = getAccessor(CLASS_REACTIVE_FILTERED_ROUTER_FUNCTION, "filterFunction");
    private static final String TYPE_HANDLER_FUNCTION = "org.springframework.web.servlet.function.HandlerFunction";
    private static final Class<?> CLASS_HANDLER_FUNCTION = loadClass(TYPE_HANDLER_FUNCTION, CLASS_LOADER);
    private static final String TYPE_RESOURCE_HANDLER_FUNCTION = "org.springframework.web.servlet.function.ResourceHandlerFunction";
    private static final Class<?> CLASS_RESOURCE_HANDLER_FUNCTION = loadClass(TYPE_RESOURCE_HANDLER_FUNCTION, CLASS_LOADER);
    private static final String TYPE_REACTIVE_HANDLER_FUNCTION = "org.springframework.web.reactive.function.server.HandlerFunction";
    private static final String TYPE_REACTIVE_HANDLER_FILTER_FUNCTION_LAMBADA = "org.springframework.web.reactive.function.server.HandlerFilterFunction$$Lambda$";
    private static final Class<?> CLASS_REACTIVE_HANDLER_FUNCTION = loadClass(TYPE_REACTIVE_HANDLER_FUNCTION, CLASS_LOADER);
    private static final String TYPE_REACTIVE_RESOURCE_HANDLER_FUNCTION = "org.springframework.web.reactive.function.server.ResourceHandlerFunction";
    private static final Class<?> CLASS_REACTIVE_RESOURCE_HANDLER_FUNCTION = loadClass(TYPE_REACTIVE_RESOURCE_HANDLER_FUNCTION, CLASS_LOADER);
    private static final Class<?> CLASS_SERVER_REQUEST = loadClass("org.springframework.web.reactive.function.server.ServerRequest", CLASS_LOADER);
    private static final Map<Object, Boolean> RESOURCE_HANDLERS = new ConcurrentHashMap<>();
    private static final Map<Object, Optional<Object>> FILTER_FUNCTIONS = new ConcurrentHashMap<>();

    public static final Method METHOD_HANDLE = ClassUtils.getDeclaredMethod(CLASS_REACTIVE_HANDLER_FUNCTION, "handle", new Class[]{CLASS_SERVER_REQUEST});


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
        return CLASS_HANDLER_METHOD != null && CLASS_HANDLER_METHOD.isInstance(handlerMethod)
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
        if (CLASS_ROUTER_FUNCTION_BUILDER != null && CLASS_ROUTER_FUNCTION_BUILDER.isInstance(builder)) {
            return (T) ACCESSOR_ERROR_HANDLERS.get(builder);
        } else if (CLASS_REACTIVE_ROUTER_FUNCTION_BUILDER != null && CLASS_REACTIVE_ROUTER_FUNCTION_BUILDER.isInstance(builder)) {
            return (T) ACCESSOR_REACTIVE_ERROR_HANDLERS.get(builder);
        }
        return null;
    }

    /**
     * Determines if a handler function is a resource handler function.
     * Uses reflection to inspect the handler's internal structure.
     *
     * @param handler The handler function to check
     * @return true if the handler is a resource handler function, false otherwise
     */
    public static boolean isResourceHandlerFunction(Object handler) {
        if (handler == null) {
            return false;
        }
        Class<?> type = getResourceType(handler);
        return type != null && RESOURCE_HANDLERS.computeIfAbsent(handler, k -> {
            try {
                Field field = k.getClass().getDeclaredField("arg$2");
                field.setAccessible(true);
                Object arg2 = field.get(k);
                return type.isInstance(arg2);
            } catch (Throwable e) {
                return false;
            }
        });
    }

    /**
     * Checks if the given handler is a reactive router function.
     *
     * @param handler The handler object to check
     * @return true if the handler is a non-null instance of a reactive router function
     */
    public static boolean isReactiveRouterFunction(Object handler) {
        return CLASS_REACTIVE_HANDLER_FUNCTION != null && CLASS_REACTIVE_HANDLER_FUNCTION.isInstance(handler);
    }

    /**
     * Extracts the reactive filter function from a handler object.
     *
     * @param handler The handler object to examine
     * @return The extracted filter function if available, null otherwise
     */
    public static Object getReactiveFilterFunction(Object handler) {
        if (CLASS_REACTIVE_FILTERED_ROUTER_FUNCTION != null && CLASS_REACTIVE_FILTERED_ROUTER_FUNCTION.isInstance(handler)) {
            return ACCESSOR_REACTIVE_FILTER_FUNCTION == null ? null : ACCESSOR_REACTIVE_FILTER_FUNCTION.get(handler);
        } else if (CLASS_REACTIVE_HANDLER_FUNCTION != null
                && CLASS_REACTIVE_HANDLER_FUNCTION.isInstance(handler)
                && handler.getClass().getName().startsWith(TYPE_REACTIVE_HANDLER_FILTER_FUNCTION_LAMBADA)) {
            return FILTER_FUNCTIONS.computeIfAbsent(handler, h -> {
                try {
                    Field field = h.getClass().getDeclaredField("arg$1");
                    field.setAccessible(true);
                    return Optional.ofNullable(field.get(h));
                } catch (Throwable e) {
                    return Optional.empty();
                }
            }).orElse(null);
        }
        return null;
    }

    /**
     * Determines the resource type based on the handler instance.
     *
     * @param handler The handler object to examine
     * @return The resource class type if the handler is a recognized function type, null otherwise
     */
    private static Class<?> getResourceType(Object handler) {
        if (CLASS_HANDLER_FUNCTION != null && CLASS_HANDLER_FUNCTION.isInstance(handler)) {
            return CLASS_RESOURCE_HANDLER_FUNCTION;
        } else if (CLASS_REACTIVE_HANDLER_FUNCTION != null && CLASS_REACTIVE_HANDLER_FUNCTION.isInstance(handler)) {
            return CLASS_REACTIVE_RESOURCE_HANDLER_FUNCTION;
        }
        return null;
    }
}
