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
import com.jd.live.agent.governance.request.HostTransformer;
import com.jd.live.agent.plugin.router.springcloud.v3.request.BlockingClientHttpRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.util.CloudUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.support.HttpAccessor;

import java.net.URI;

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
        // do not static import CloudUtils to avoid class loading issue.
        if (!CloudUtils.isCloudEnabled() || !CloudUtils.isBlockingCloudClient(template)) {
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

}
