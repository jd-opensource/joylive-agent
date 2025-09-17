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
package com.jd.live.agent.plugin.router.springcloud.v2_2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.request.HostTransformer;
import com.jd.live.agent.plugin.router.springcloud.v2_2.request.BlockingClientHttpRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.HttpAccessor;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;
import static com.jd.live.agent.plugin.router.springcloud.v2_2.condition.ConditionalOnSpringCloud2Enabled.TYPE_SERVICE_INSTANCE_LIST_SUPPLIER;

/**
 * BlockingClientInterceptor
 */
public class BlockingClientInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public BlockingClientInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        HttpAccessor template = (HttpAccessor) ctx.getTarget();
        URI uri = ctx.getArgument(0);
        HttpMethod method = ctx.getArgument(1);
        if (Accessor.isCloudEnabled()) {
            // with spring cloud
            if (!Accessor.isCloudClient(template)) {
                HostTransformer transformer = context.getHostTransformer(uri.getHost());
                if (transformer != null) {
                    // Handle multi-active and lane domains
                    mc.skipWithResult(new BlockingClientHttpRequest(uri, method, null, template, transformer, context));
                }
            }
        } else {
            // only spring boot
            String service = context.isMicroserviceTransformEnabled() ? context.getService(uri) : null;
            if (service != null && !service.isEmpty()) {
                // Convert regular spring web requests to microservice calls
                mc.skipWithResult(new BlockingClientHttpRequest(uri, method, service, template, null, context));
            } else {
                // Handle multi-active and lane domains
                HostTransformer transformer = context.getHostTransformer(uri.getHost());
                if (transformer != null) {
                    // Handle multi-active and lane domains
                    mc.skipWithResult(new BlockingClientHttpRequest(uri, method, null, template, transformer, context));
                }
            }
        }
    }

    /**
     * Utility class for detecting Spring Cloud environment and load balancer configuration.
     */
    private static class Accessor {

        // spring cloud 3+
        private static final Class<?> lbType = loadClass(TYPE_SERVICE_INSTANCE_LIST_SUPPLIER, HttpAccessor.class.getClassLoader());

        /**
         * Checks if Spring Cloud is available in the classpath.
         *
         * @return true if Spring Cloud is present, false otherwise
         */
        public static boolean isCloudEnabled() {
            return lbType != null;
        }

        /**
         * Determines if the RestTemplate is configured as a load-balanced client.
         *
         * @param client the RestTemplate to check
         * @return true if configured with load balancer interceptors, false otherwise
         */
        public static boolean isCloudClient(Object client) {
            // Parameter client cannot be declared as RestTemplate, as it will cause class loading exceptions.
            if (client instanceof RestTemplate) {
                RestTemplate template = (RestTemplate) client;
                for (ClientHttpRequestInterceptor interceptor : template.getInterceptors()) {
                    if (interceptor instanceof RetryLoadBalancerInterceptor) {
                        return true;
                    } else if (interceptor instanceof LoadBalancerInterceptor) {
                        return true;
                    }
                }
            }

            return false;
        }

    }

}
