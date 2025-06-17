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
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * TomcatWebServerFactoryInterceptor
 */
public class TomcatWebServerFactoryInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(TomcatWebServerFactoryInterceptor.class);

    @Override
    public void onSuccess(ExecutableContext ctx) {
        Connector connector = ctx.getArgument(0);
        ProtocolHandler protocolHandler = connector.getProtocolHandler();
        Executor executor = protocolHandler.getExecutor();
        Object factory = getQuietly(executor, "factory");
        if (factory == null || !factory.getClass().getName().equals("java.lang.ThreadBuilders$VirtualThreadFactory")) {
            try {
                Method method = Thread.class.getDeclaredMethod("ofVirtual");
                Object ofVirtual = method.invoke(null);
                Class<?> ofVirtualClass = ofVirtual.getClass();
                Method nameMethod = ofVirtualClass.getDeclaredMethod("name", String.class, long.class);
                nameMethod.setAccessible(true);
                ofVirtual = nameMethod.invoke(ofVirtual, "tomcat-vt-", 1);
                Method factoryMethod = ofVirtualClass.getDeclaredMethod("factory");
                factoryMethod.setAccessible(true);
                factory = factoryMethod.invoke(ofVirtual);
                Method executorMethod = Executors.class.getDeclaredMethod("newThreadPerTaskExecutor", ThreadFactory.class);
                executor = (Executor) executorMethod.invoke(null, (ThreadFactory) factory);
                protocolHandler.setExecutor(executor);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                logger.error("Failed to set tomcat virtual thread, caused by {}", cause.getMessage(), cause);
            } catch (Throwable e) {
                logger.error("Failed to set tomcat virtual thread, caused by {}", e.getMessage(), e);
            }
        }
    }

}
