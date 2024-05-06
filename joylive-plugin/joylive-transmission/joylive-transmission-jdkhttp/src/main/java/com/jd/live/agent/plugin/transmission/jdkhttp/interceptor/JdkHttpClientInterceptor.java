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
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.governance.context.RequestContext;

import java.lang.reflect.Method;
import java.util.List;

public class JdkHttpClientInterceptor extends InterceptorAdaptor {

    private static final String TYPE_MESSAGE_HEADER = "sun.net.www.MessageHeader";

    private static final String METHOD_ADD = "add";

    private final Method method;

    public JdkHttpClientInterceptor() {
        // use reflect to avoid module error in java 17.
        Method method = null;
        try {
            List<Method> methods = ClassUtils.describe(Class.forName(TYPE_MESSAGE_HEADER)).getMethodList().getMethods(METHOD_ADD);
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

    private void attachTag(Object header) {
        RequestContext.traverse((key, value) -> addHeader(header, key, value));
    }

    private void addHeader(Object header, String key, String value) {
        try {
            method.invoke(header, key, value);
        } catch (Throwable ignore) {
        }
    }

}
