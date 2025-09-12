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
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springcloud.v3.request.BlockingWebHttpRequest;
import org.springframework.http.client.support.HttpAccessor;

import java.net.URI;

import static com.jd.live.agent.core.Constants.PREDICATE_LB;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * BlockingWebClusterInterceptor
 */
public class BlockingWebClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final GovernanceConfig config;

    public BlockingWebClusterInterceptor(InvocationContext context) {
        this.context = context;
        this.config = context.getGovernanceConfig();
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        URI uri = ctx.getArgument(0);
        if (Accessor.lbType != null) {
            // with spring cloud
            String scheme = uri.getScheme();
            if (scheme != null && !scheme.isEmpty() && !PREDICATE_LB.test(scheme) && context.isLocationEnabled()) {
                // web request
                mc.skipWithResult(new BlockingWebHttpRequest(uri, ctx.getArgument(1), null, (HttpAccessor) mc.getTarget(), context));
            }
        } else {
            // only spring boot
            String service = config.getRegistryConfig().isEnabled() && context.isGovernEnabled() ? context.getService(uri) : null;
            if (service != null && !service.isEmpty() || context.isLocationEnabled()) {
                mc.skipWithResult(new BlockingWebHttpRequest(uri, ctx.getArgument(1), service, (HttpAccessor) mc.getTarget(), context));
            }
        }
    }

    private static class Accessor {

        private static Class<?> lbType = loadClass("org.springframework.cloud.client.loadbalancer.LoadBalanced", HttpAccessor.class.getClassLoader());

    }

}
