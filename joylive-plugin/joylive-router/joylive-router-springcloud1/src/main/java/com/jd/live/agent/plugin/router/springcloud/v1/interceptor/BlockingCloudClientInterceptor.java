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
package com.jd.live.agent.plugin.router.springcloud.v1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.plugin.router.springcloud.v1.cluster.BlockingCloudCluster;
import com.jd.live.agent.plugin.router.springcloud.v1.request.BlockingCloudClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v1.response.BlockingClusterResponse;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BlockingCloudClientInterceptor
 *
 * @since 1.0.0
 */
public class BlockingCloudClientInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public BlockingCloudClientInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        BlockingCloudCluster cluster = Accessor.clusters.computeIfAbsent((ClientHttpRequestInterceptor) ctx.getTarget(), i -> new BlockingCloudCluster(context.getRegistry(), i));
        BlockingCloudClusterRequest request = new BlockingCloudClusterRequest(
                ctx.getArgument(0),
                ctx.getArgument(1),
                ctx.getArgument(2),
                cluster.getContext());
        HttpOutboundInvocation<BlockingCloudClusterRequest> invocation = new HttpOutboundInvocation<>(request, context);
        BlockingClusterResponse response = cluster.request(invocation);
        ServiceError error = response.getError();
        if (error != null && !error.isServerError()) {
            mc.skipWithThrowable(error.getThrowable());
        } else {
            mc.skipWithResult(response.getResponse());
        }
    }

    private static class Accessor {
        private static final Map<ClientHttpRequestInterceptor, BlockingCloudCluster> clusters = new ConcurrentHashMap<>();
    }
}
