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
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.web.context.request.RequestContextHolder;

import java.lang.reflect.Method;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredMethod;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * Base factory class for handling servlet-related parameter resolution.
 * Provides utility methods to access servlet request/response attributes and HTTP methods.
 */
public abstract class ServletParameterFactory {

    protected static final String TYPE_SERVLET_REQUEST_ATTRIBUTES = "org.springframework.web.context.request.ServletRequestAttributes";
    protected static final Class<?> CLASS_SERVLET_REQUEST_ATTRIBUTES = loadClass(TYPE_SERVLET_REQUEST_ATTRIBUTES, ResourceLoader.class.getClassLoader());
    protected static final FieldAccessor ACCESSOR_REQUEST = getAccessor(CLASS_SERVLET_REQUEST_ATTRIBUTES, "request");
    protected static final FieldAccessor ACCESSOR_RESPONSE = getAccessor(CLASS_SERVLET_REQUEST_ATTRIBUTES, "response");
    protected static final Method METHOD_HTTP_METHOD_RESOLVE = getDeclaredMethod(HttpMethod.class, "resolve", new Class[]{String.class});
    protected static final Method METHOD_HTTP_METHOD_VALUE_OF = getDeclaredMethod(HttpMethod.class, "valueOf", new Class[]{String.class});

    protected Object getRequestContext() {
        return RequestContextHolder.getRequestAttributes();
    }

    /**
     * Gets a value using the specified accessor from an object.
     *
     * @param obj      the source object
     * @param accessor the field accessor
     * @param type     the expected return type
     * @return the value of type T
     */
    protected <T> T get(Object obj, FieldAccessor accessor, Class<T> type) {
        return obj == null ? null : accessor.get(obj, type);
    }

    /**
     * Gets the web request cast to the specified type.
     *
     * @param type the expected type
     * @return the web request as type T, or null if not compatible
     */
    protected <T> T getWebRequest(Class<T> type) {
        return get(getRequestContext(), ACCESSOR_REQUEST, type);
    }

    /**
     * Retrieves the current web response and casts it to the specified type.
     *
     * @param <T>  the target type parameter
     * @param type the class to cast the response to
     * @return the web response as the specified type, or null if not compatible
     */
    protected <T> T getWebResponse(Class<T> type) {
        return get(getRequestContext(), ACCESSOR_RESPONSE, type);
    }

    /**
     * Resolves an HTTP method from its string representation.
     *
     * @param method the HTTP method string
     * @return the resolved HttpMethod object, or null if invalid
     */
    protected Object getHttpMethod(String method) {
        if (method == null) {
            return null;
        }
        try {
            return METHOD_HTTP_METHOD_RESOLVE != null
                    ? METHOD_HTTP_METHOD_RESOLVE.invoke(null, method)
                    : METHOD_HTTP_METHOD_VALUE_OF.invoke(null, method);
        } catch (Throwable e) {
            return null;
        }
    }

}
