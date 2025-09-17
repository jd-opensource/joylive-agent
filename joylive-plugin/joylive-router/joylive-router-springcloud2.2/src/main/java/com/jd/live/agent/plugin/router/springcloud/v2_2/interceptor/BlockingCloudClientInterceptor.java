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
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.plugin.router.springcloud.v2_2.cluster.BlockingCloudCluster;
import com.jd.live.agent.plugin.router.springcloud.v2_2.request.BlockingCloudClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v2_2.response.BlockingClusterResponse;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BlockingClusterInterceptor
 *
 * @since 1.0.0
 */
public class BlockingCloudClientInterceptor extends AbstractCloudClusterInterceptor<HttpRequest> {

    private final Map<ClientHttpRequestInterceptor, BlockingCloudCluster> clusters = new ConcurrentHashMap<>();

    public BlockingCloudClientInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    protected void request(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        HttpRequest request = ctx.getArgument(0);
        BlockingCloudCluster cluster = clusters.computeIfAbsent((ClientHttpRequestInterceptor) ctx.getTarget(), i -> new BlockingCloudCluster(context.getRegistry(), i));
        BlockingCloudClusterRequest clusterRequest = new BlockingCloudClusterRequest(request, ctx.getArgument(1), ctx.getArgument(2), cluster.getContext());
        HttpOutboundInvocation<BlockingCloudClusterRequest> invocation = new HttpOutboundInvocation<>(clusterRequest, context);
        BlockingClusterResponse response = cluster.request(invocation);
        ServiceError error = response.getError();
        if (error != null && !error.isServerError()) {
            mc.skipWithThrowable(error.getThrowable());
        } else {
            mc.skipWithResult(response.getResponse());
        }
    }

    @Override
    protected String getServiceName(HttpRequest request) {
        return request.getURI().getHost();
    }
}
