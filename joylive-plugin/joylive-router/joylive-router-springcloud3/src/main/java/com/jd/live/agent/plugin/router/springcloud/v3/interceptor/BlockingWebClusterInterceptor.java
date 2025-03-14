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
import com.jd.live.agent.governance.config.RegistryConfig;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.router.springcloud.v3.request.BlockingWebHttpRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.support.HttpAccessor;

import java.net.URI;

/**
 * RestTemplateInterceptor
 */
public class BlockingWebClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final Registry registry;

    private final RegistryConfig config;

    public BlockingWebClusterInterceptor(InvocationContext context, Registry registry) {
        this.context = context;
        this.registry = registry;
        this.config = context.getGovernanceConfig().getRegistryConfig();
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        HttpAccessor restTemplate = (HttpAccessor) mc.getTarget();
        URI url = ctx.getArgument(0);
        HttpMethod method = ctx.getArgument(1);
        String service = config.getService(url);
        if (service != null && !service.isEmpty()) {
            BlockingWebHttpRequest request = new BlockingWebHttpRequest(url, method, service, restTemplate, context, registry);
            mc.skipWithResult(request);
        }
    }

}
