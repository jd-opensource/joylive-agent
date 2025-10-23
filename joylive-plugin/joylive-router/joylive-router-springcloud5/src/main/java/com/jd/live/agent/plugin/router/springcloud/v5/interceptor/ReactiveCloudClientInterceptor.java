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
package com.jd.live.agent.plugin.router.springcloud.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.plugin.router.springcloud.v5.cluster.ReactiveCloudCluster;
import com.jd.live.agent.plugin.router.springcloud.v5.request.ReactiveCloudClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v5.response.ReactiveClusterResponse;
import com.jd.live.agent.plugin.router.springcloud.v5.util.CloudUtils;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;

/**
 * ReactiveClusterInterceptor
 *
 * @since 1.0.0
 */
public class ReactiveCloudClientInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public ReactiveCloudClientInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        // Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next);
        MethodContext mc = (MethodContext) ctx;
        LoadBalancedExchangeFilterFunction filter = (LoadBalancedExchangeFilterFunction) ctx.getTarget();
        // do not static import CloudUtils to avoid class loading issue.
        ReactiveCloudCluster cluster = CloudUtils.getOrCreateCluster(filter, i -> new ReactiveCloudCluster(context.getRegistry(), i));
        ReactiveCloudClusterRequest request = new ReactiveCloudClusterRequest(
                ctx.getArgument(0),
                ctx.getArgument(1),
                cluster.getContext());
        HttpOutboundInvocation<ReactiveCloudClusterRequest> invocation = new HttpOutboundInvocation<>(request, context);
        CompletionStage<ClientResponse> stage = cluster.invoke(invocation, ReactiveClusterResponse::getResponse);
        mc.skipWithResult(Mono.fromCompletionStage(stage));
    }
}
