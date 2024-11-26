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
package com.jd.live.agent.plugin.transimission.nettyhttp.v1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.governance.context.RequestContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.handler.codec.http.HttpMethod;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

/**
 * NettyHttpClientInterceptor
 */
@Deprecated
public class NettyHttpClientInterceptor extends InterceptorAdaptor {


    /**
     * Enhanced logic after method successfully execute
     *
     * @param ctx ExecutableContext
     * @see reactor.netty.http.client.HttpClient#request(HttpMethod)
     */
    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        HttpClient client = mc.getResult();
        if (RequestContext.hasCargo()) {
            HttpClient newClient = client.headers(headers -> RequestContext.cargos(cargo -> headers.set(cargo.getKey(), cargo.getValue())));
            if (newClient.getClass() == client.getClass()) {
                mc.setResult(newClient);
            } else {
                // for netty reactor 0.9.20.RELEASE
                try {
                    mc.setResult(Reflection.INSTANCE.invoke(newClient, mc.getArgument(0)));
                } catch (Exception ignored) {
                }
            }
        }
    }


    private static class Reflection {

        private static final Reflection INSTANCE = new Reflection();

        private final Method tcpConfiguration;

        private final Method httpMethod;

        private final Constructor<?> constructor;

        Reflection() {
            this.tcpConfiguration = getTcpConfiguration();
            this.httpMethod = getHttpMethod();
            this.constructor = getConstructor();
        }

        private Constructor<?> getConstructor() {
            Class<?> clazz = ClassUtils.loadClass("reactor.netty.http.client.HttpClientFinalizer", HttpClient.class.getClassLoader());
            if (clazz != null) {
                Constructor<?> c = ClassUtils.describe(clazz).getConstructorList().getDefaultSingleConstructor();
                if (c != null) {
                    c.setAccessible(true);
                    return c;
                }
            }
            return null;
        }

        private Method getTcpConfiguration() {
            Class<?> clazz = ClassUtils.loadClass("reactor.netty.http.client.HttpClientHeaders", HttpClient.class.getClassLoader());
            if (clazz != null) {
                List<Method> methods = ClassUtils.describe(clazz).getMethodList().getMethods("tcpConfiguration");
                if (methods != null) {
                    for (Method method : methods) {
                        if (method.getParameterCount() == 0) {
                            method.setAccessible(true);
                            return method;
                        }
                    }
                }
            }
            return null;
        }

        private Method getHttpMethod() {
            Class<?> clazz = ClassUtils.loadClass("reactor.netty.http.client.HttpClientConfiguration", HttpClient.class.getClassLoader());
            if (clazz != null) {
                List<Method> methods = ClassUtils.describe(clazz).getMethodList().getMethods("method");
                if (methods != null && !methods.isEmpty()) {
                    Method method = methods.get(0);
                    method.setAccessible(true);
                    return method;
                }
            }
            return null;
        }

        public Object invoke(Object obj, HttpMethod method) throws Exception {
            TcpClient tcpClient = tcpConfiguration == null ? null : (TcpClient) tcpConfiguration.invoke(obj);
            TcpClient tcpConfiguration = tcpClient == null ? null : tcpClient.bootstrap(b -> {
                try {
                    return (Bootstrap) httpMethod.invoke(null, b, method);
                } catch (Throwable ignored) {
                    return b;
                }
            });
            return constructor == null || tcpConfiguration == null ? null : constructor.newInstance(tcpConfiguration);
        }

    }
}
