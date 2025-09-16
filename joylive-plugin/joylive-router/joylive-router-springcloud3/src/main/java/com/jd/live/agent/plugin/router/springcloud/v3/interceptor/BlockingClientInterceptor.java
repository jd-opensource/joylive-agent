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
package com.jd.live.agent.plugin.router.springcloud.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springcloud.v3.request.BlockingClientHttpRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.HttpAccessor;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;
import static com.jd.live.agent.plugin.router.springcloud.v3.condition.ConditionalOnSpringCloud3Enabled.TYPE_HINT_REQUEST_CONTEXT;

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
        RestTemplate template = (RestTemplate) ctx.getTarget();
        MethodContext mc = (MethodContext) ctx;
        URI uri = ctx.getArgument(0);
        if (Accessor.isCloudEnabled()) {
            // with spring cloud
            if (!Accessor.isCloudClient(template) && context.isLocationEnabled()) {
                // web request
                mc.skipWithResult(new BlockingClientHttpRequest(uri, ctx.getArgument(1), null, (HttpAccessor) mc.getTarget(), context));
            }
        } else {
            // only spring boot
            String service = context.isRegistryEnabled() && context.isFlowControlEnabled() ? context.getService(uri) : null;
            if (service != null && !service.isEmpty() || context.isLocationEnabled()) {
                mc.skipWithResult(new BlockingClientHttpRequest(uri, ctx.getArgument(1), service, (HttpAccessor) mc.getTarget(), context));
            }
        }
    }

    /**
     * Utility class for detecting Spring Cloud environment and load balancer configuration.
     */
    private static class Accessor {

        // spring cloud 3+
        private static final Class<?> lbType = loadClass(TYPE_HINT_REQUEST_CONTEXT, HttpAccessor.class.getClassLoader());

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
         * @param template the RestTemplate to check
         * @return true if configured with load balancer interceptors, false otherwise
         */
        public static boolean isCloudClient(RestTemplate template) {
            for (ClientHttpRequestInterceptor interceptor : template.getInterceptors()) {
                if (interceptor instanceof RetryLoadBalancerInterceptor) {
                    return true;
                } else if (interceptor instanceof LoadBalancerInterceptor) {
                    return true;
                }
            }
            return false;
        }

    }

}
