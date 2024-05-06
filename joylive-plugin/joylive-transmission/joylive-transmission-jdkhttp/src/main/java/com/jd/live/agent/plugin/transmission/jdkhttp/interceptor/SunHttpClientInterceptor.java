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
 * Intercepts HTTP requests made by {@code sun.net.www.http.HttpClient} to
 * add custom headers to the request. This is achieved by using reflection
 * to invoke the {@code add} method on {@code sun.net.www.MessageHeader}.
 *
 * <p>The use of reflection allows this interceptor to modify the request headers
 * without being hindered by the module system introduced in Java 9, which restricts
 * access to JDK internal APIs.</p>
 *
 * <p>This class is part of an instrumentation plugin designed to monitor or modify
 * the behavior of HTTP requests for observability or governance purposes.</p>
 */
public class SunHttpClientInterceptor extends InterceptorAdaptor {

    private static final String TYPE_MESSAGE_HEADER = "sun.net.www.MessageHeader";

    private static final String METHOD_ADD = "add";

    /**
     * Method reference for {@code sun.net.www.MessageHeader#add(String, String)}.
     * This is obtained via reflection to bypass module system restrictions.
     */
    private final Method method;

    /**
     * Constructs a new {@code SunHttpClientInterceptor}.
     * Attempts to obtain a reference to the {@code add} method of
     * {@code sun.net.www.MessageHeader} via reflection.
     */
    public SunHttpClientInterceptor() {
        // use reflect to avoid module error in java 17.
        Method method = null;
        try {
            List<Method> methods = describe(Class.forName(TYPE_MESSAGE_HEADER))
                    .getMethodList()
                    .getMethods(METHOD_ADD);
            method = methods == null || methods.isEmpty() ? null : methods.get(0);
        } catch (ClassNotFoundException ignore) {
        }
        this.method = method;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        if (method != null) {
            attachTag(ctx.getArguments()[0]);
        }
    }

    /**
     * Attaches custom headers to the HTTP request.
     *
     * @param header The {@code sun.net.www.MessageHeader} instance to which headers are added.
     */
    private void attachTag(Object header) {
        RequestContext.traverse((key, value) -> addHeader(header, key, value));
    }

    /**
     * Adds a single header to the HTTP request.
     *
     * @param header The {@code sun.net.www.MessageHeader} instance to which a header is added.
     * @param key    The name of the header.
     * @param value  The value of the header.
     */
    private void addHeader(Object header, String key, String value) {
        try {
            method.invoke(header, key, value);
        } catch (Throwable ignore) {
        }
    }

}
