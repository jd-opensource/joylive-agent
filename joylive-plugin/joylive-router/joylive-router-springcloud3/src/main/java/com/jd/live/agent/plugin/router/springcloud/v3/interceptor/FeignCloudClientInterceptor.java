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
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.plugin.router.springcloud.v3.cluster.FeignCloudCluster;
import com.jd.live.agent.plugin.router.springcloud.v3.request.FeignCloudClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.response.FeignClusterResponse;
import com.jd.live.agent.plugin.router.springcloud.v3.util.CloudUtils;
import feign.Client;

/**
 * FeignCloudClientInterceptor
 *
 * @since 1.0.0
 */
public class FeignCloudClientInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public FeignCloudClientInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Client client = (Client) ctx.getTarget();
        // do not static import CloudUtils to avoid class loading issue.
        FeignCloudCluster cluster = CloudUtils.getOrCreateCluster(client, i -> new FeignCloudCluster(context.getRegistry(), i));
        FeignCloudClusterRequest request = new FeignCloudClusterRequest(
                ctx.getArgument(0),
                ctx.getArgument(1),
                cluster.getContext());
        HttpOutboundInvocation<FeignCloudClusterRequest> invocation = new HttpOutboundInvocation<>(request, context);
        FeignClusterResponse response = cluster.request(invocation);
        // FeignClusterResponse implement ResultProvider
        mc.skipWith(response);
    }
}
