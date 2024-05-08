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
package com.jd.live.agent.plugin.transmission.jdkhttp.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;

import java.lang.reflect.Method;
import java.util.List;

import static com.jd.live.agent.core.util.type.ClassUtils.describe;

/**
 * Interceptor for the Java HTTP Client's request builder implementation.
 * This class uses reflection to access and modify the headers of HTTP requests
 * constructed by the internal {@code HttpRequestBuilderImpl} class.
 *
 * <p>Due to module encapsulation in Java 9 and above, direct access to the internal
 * HTTP request builder is not possible. This interceptor bypasses these restrictions
 * to allow header manipulation for outgoing HTTP requests.</p>
 *
 * <p>Note: This interceptor is designed to work with specific versions of the JDK
 * where {@code HttpRequestBuilderImpl} and its method {@code setHeader} are present.</p>
 */
public class JavaHttpClientInterceptor extends InterceptorAdaptor {

    private static final String TYPE_HTTP_REQUEST_BUILDER_IMPL = "jdk.internal.net.http.HttpRequestBuilderImpl";

    private static final String METHOD_SET_HEADER = "setHeader";

    private final Method method;

    public JavaHttpClientInterceptor() {
        // use reflect to avoid module error in java 17.
        Method method = null;
        try {
            // Use reflection to get the setHeader method from HttpRequestBuilderImpl
            List<Method> methods = describe(Class.forName(TYPE_HTTP_REQUEST_BUILDER_IMPL))
                    .getMethodList()
                    .getMethods(METHOD_SET_HEADER);
            method = methods == null || methods.isEmpty() ? null : methods.get(0);
        } catch (ClassNotFoundException ignore) {
        }
        this.method = method;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        if (method != null) {
            attachTag(ctx.getTarget());
        }
    }

    /**
     * Attaches tags to the HTTP header using the {@code setHeader} method.
     *
     * @param header The HTTP request header object to which the tags will be attached.
     */
    private void attachTag(Object header) {
        RequestContext.cargos((key, value) -> addHeader(header, key, value));
    }

    /**
     * Adds a single header to the HTTP request.
     *
     * @param header The HTTP request header object.
     * @param key The header key.
     * @param value The header value.
     */
    private void addHeader(Object header, String key, String value) {
        try {
            method.invoke(header, key, value);
        } catch (Throwable ignore) {
        }
    }

}
