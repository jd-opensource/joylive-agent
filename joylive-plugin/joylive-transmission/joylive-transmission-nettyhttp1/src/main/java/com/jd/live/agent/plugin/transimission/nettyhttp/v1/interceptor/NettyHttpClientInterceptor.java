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
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.ExceptionUtils;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.plugin.transimission.nettyhttp.v1.request.HttpHeadersWriter;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientConfig;
import reactor.netty.tcp.TcpClient;

import java.lang.reflect.Method;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredMethod;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * NettyHttpClientInterceptor
 */
public class NettyHttpClientInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(NettyHttpClientInterceptor.class);

    private final Propagation propagation;

    public NettyHttpClientInterceptor(Propagation propagation) {
        this.propagation = propagation;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        Accessor.transmit(((MethodContext) ctx).getResult(), propagation, RequestContext.get());
    }

    private static class Accessor {

        private static final ClassLoader classLoader = HttpClient.class.getClassLoader();

        private static final Class<?> connectClass = loadClass("reactor.netty.http.client.HttpClientConnect", classLoader);

        private static final Class<?> finalizerClass = loadClass("reactor.netty.http.client.HttpClientFinalizer", classLoader);

        private static final Class<?> configurationClass = loadClass("reactor.netty.http.client.HttpClientConfiguration", classLoader);

        private static final Class<?> configClass = loadClass("reactor.netty.http.client.HttpClientConfig", classLoader);

        private static final Class<?> bootstrapClass = loadClass("io.netty.bootstrap.Bootstrap", classLoader);

        private static final Class<?> headersClass = loadClass("io.netty.handler.codec.http.HttpHeaders", classLoader);

        private static final FieldAccessor configAccessor = getAccessor(connectClass, "config");

        private static final FieldAccessor headersAccessor = getAccessor(configClass, "headers");

        private static final FieldAccessor configurationAccessor = getAccessor(finalizerClass, "cachedConfiguration");

        private static final Method headersGetter = getDeclaredMethod(configurationClass, "headers", new Class[]{bootstrapClass});

        private static final Method headersSetter = getDeclaredMethod(configurationClass, "headers", new Class[]{bootstrapClass, headersClass});

        private static void transmit(Object result, Propagation propagation, Carrier carrier) {
            if (connectClass != null && connectClass.isInstance(result) && configAccessor != null && headersAccessor != null) {
                transmitV1(result, propagation, carrier);
            } else if (finalizerClass != null && finalizerClass.isInstance(result) && configurationAccessor != null && headersGetter != null && headersSetter != null) {
                transmitV0(result, propagation, carrier);
            }
        }

        private static void transmitV1(Object result, Propagation propagation, Carrier carrier) {
            // netty reactor 1.x
            Object config = configAccessor.get(result);
            if (config instanceof HttpClientConfig) {
                // it will copy headers
                HttpHeaders headers = ((HttpClientConfig) config).headers();
                propagation.write(carrier, new HttpHeadersWriter(headers));
                headersAccessor.set(config, headers);
            }
        }

        private static void transmitV0(Object result, Propagation propagation, Carrier carrier) {
            // netty reactor 0.x
            Object configuration = configurationAccessor.get(result);
            if (configuration instanceof TcpClient) {
                TcpClient newClient = ((TcpClient) configuration).bootstrap(b -> {
                    try {
                        HttpHeaders headers = (HttpHeaders) headersGetter.invoke(null, b);
                        if (headers == null) {
                            headers = new DefaultHttpHeaders();
                        }
                        propagation.write(carrier, new HttpHeadersWriter(headers));
                        if (!headers.isEmpty()) {
                            headersSetter.invoke(null, b, headers);
                        }
                    } catch (Throwable e) {
                        Throwable cause = ExceptionUtils.getCause(e);
                        logger.error("Failed to propagate headers, caused by ", cause.getMessage(), cause);
                    }
                    return b;
                });
                configurationAccessor.set(result, newClient);
            }
        }

    }
}
