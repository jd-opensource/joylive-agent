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
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.plugin.router.springcloud.v3.cluster.BlockingCloudCluster;
import com.jd.live.agent.plugin.router.springcloud.v3.request.BlockingCloudClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.response.BlockingClusterResponse;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BlockingClusterInterceptor
 *
 * @since 1.0.0
 */
public class BlockingCloudClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final Map<ClientHttpRequestInterceptor, BlockingCloudCluster> clusters = new ConcurrentHashMap<>();

    public BlockingCloudClusterInterceptor(InvocationContext context) {
        this.context = context;
    }

    /**
     * Enhanced logic before method execution
     *
     * @param ctx The execution context of the method being intercepted.
     * @see org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor#intercept(HttpRequest, byte[], ClientHttpRequestExecution)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        BlockingCloudCluster cluster = clusters.computeIfAbsent((ClientHttpRequestInterceptor) ctx.getTarget(), BlockingCloudCluster::new);
        BlockingCloudClusterRequest request = new BlockingCloudClusterRequest(ctx.getArgument(0),
                ctx.getArgument(1), ctx.getArgument(2), cluster.getContext());
        HttpOutboundInvocation<BlockingCloudClusterRequest> invocation = new HttpOutboundInvocation<>(request, context);
        BlockingClusterResponse response = cluster.request(invocation);
        ServiceError error = response.getError();
        if (error != null && !error.isServerError()) {
            mc.skipWithThrowable(error.getThrowable());
        } else {
            mc.skipWithResult(response.getResponse());
        }
    }
}
