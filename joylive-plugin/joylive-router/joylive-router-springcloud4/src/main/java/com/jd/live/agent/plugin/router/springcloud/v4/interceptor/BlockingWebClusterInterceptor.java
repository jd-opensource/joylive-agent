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
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.router.springcloud.v4.request.BlockingWebHttpRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.support.HttpAccessor;

import java.net.URI;

/**
 * BlockingWebClusterInterceptor
 */
public class BlockingWebClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final Registry registry;

    public BlockingWebClusterInterceptor(InvocationContext context, Registry registry) {
        this.context = context;
        this.registry = registry;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        HttpAccessor restTemplate = (HttpAccessor) mc.getTarget();
        URI uri = ctx.getArgument(0);
        HttpMethod method = ctx.getArgument(1);
        String service = context.getService(uri);
        if (service != null && !service.isEmpty()) {
            BlockingWebHttpRequest request = new BlockingWebHttpRequest(uri, method, service, restTemplate, context, registry);
            mc.skipWithResult(request);
        }
    }

}
