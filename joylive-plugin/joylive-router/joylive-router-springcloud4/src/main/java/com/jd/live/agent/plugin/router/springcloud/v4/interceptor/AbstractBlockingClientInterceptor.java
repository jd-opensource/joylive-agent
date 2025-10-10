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
package com.jd.live.agent.plugin.router.springcloud.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.request.HostTransformer;
import com.jd.live.agent.plugin.router.springcloud.v4.request.BlockingClientHttpRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.request.BlockingClientHttpRequestBuilder;
import com.jd.live.agent.plugin.router.springcloud.v4.util.CloudUtils;

import java.net.URI;

/**
 * AbstractBlockingClientInterceptor
 */
public abstract class AbstractBlockingClientInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public AbstractBlockingClientInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        BlockingClientHttpRequestBuilder builder = builder(ctx);
        URI uri = builder.getUri();
        // do not static import CloudUtils to avoid class loading issue.
        if (CloudUtils.isCloudEnabled()) {
            // with spring cloud
            if (!CloudUtils.isBlockingCloudClient(builder.getInterceptors())) {
                HostTransformer transformer = context.getHostTransformer(uri.getHost());
                if (transformer != null) {
                    // Handle multi-active and lane domains
                    mc.skipWithResult(new BlockingClientHttpRequest(builder, null, transformer, context));
                }
            }
        } else {
            // only spring boot
            String service = context.isMicroserviceTransformEnabled() ? context.getService(uri) : null;
            if (service != null && !service.isEmpty()) {
                // Convert regular spring web requests to microservice calls
                mc.skipWithResult(new BlockingClientHttpRequest(builder, service, null, context));
            } else {
                // Handle multi-active and lane domains
                HostTransformer transformer = context.getHostTransformer(uri.getHost());
                if (transformer != null) {
                    // Handle multi-active and lane domains
                    mc.skipWithResult(new BlockingClientHttpRequest(builder, null, transformer, context));
                }
            }
        }
    }

    /**
     * Creates a request specification from the execution context.
     *
     * @param ctx the execution context
     * @return the request specification
     */
    protected abstract BlockingClientHttpRequestBuilder builder(ExecutableContext ctx);

}
