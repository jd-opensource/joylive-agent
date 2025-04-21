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
package com.jd.live.agent.plugin.application.springboot.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.bootstrap.AppListener;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.application.springboot.v2.listener.InnerListener;
import com.jd.live.agent.plugin.application.springboot.v2.util.AppLifecycle;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

public class ApplicationLoadInterceptor extends InterceptorAdaptor {

    private final AppListener listener;

    public ApplicationLoadInterceptor(AppListener listener) {
        this.listener = listener;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        // fix for spring boot 2.1, it will trigger twice.
        AppLifecycle.load(() -> {
            ResourceLoader resourceLoader = ctx.getArgument(0);
            ClassLoader classLoader = resourceLoader != null ? resourceLoader.getClassLoader() : ClassUtils.getDefaultClassLoader();
            Class<?> mainClass = deduceMainApplicationClass();
            InnerListener.foreach(l -> l.onLoading(classLoader, mainClass));
            listener.onLoading(classLoader, mainClass);
        });
    }

    private Class<?> deduceMainApplicationClass() {
        try {
            StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                if ("main".equals(stackTraceElement.getMethodName())) {
                    return Class.forName(stackTraceElement.getClassName());
                }
            }
        } catch (ClassNotFoundException ex) {
            // Swallow and continue
        }
        return null;
    }
}
