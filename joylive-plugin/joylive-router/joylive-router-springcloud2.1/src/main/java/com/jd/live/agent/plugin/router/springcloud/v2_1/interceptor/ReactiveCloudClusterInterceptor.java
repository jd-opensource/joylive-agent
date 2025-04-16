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
package com.jd.live.agent.plugin.router.springcloud.v2_1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.plugin.router.springcloud.v2_1.cluster.ReactiveCloudCluster;
import com.jd.live.agent.plugin.router.springcloud.v2_1.request.ReactiveCloudClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v2_1.response.ReactiveClusterResponse;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ReactiveClusterInterceptor
 *
 * @since 1.0.0
 */
public class ReactiveCloudClusterInterceptor extends AbstractCloudClusterInterceptor<ClientRequest> {

    private final Map<ExchangeFilterFunction, ReactiveCloudCluster> clusters = new ConcurrentHashMap<>();

    public ReactiveCloudClusterInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    protected void request(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        ClientRequest request = ctx.getArgument(0);
        ExchangeFilterFunction filter = (ExchangeFilterFunction) ctx.getTarget();
        ReactiveCloudCluster cluster = clusters.computeIfAbsent(filter, i -> new ReactiveCloudCluster(context.getRegistry(), i));
        ReactiveCloudClusterRequest clusterRequest = new ReactiveCloudClusterRequest(request, ctx.getArgument(1), cluster.getContext());
        HttpOutboundInvocation<ReactiveCloudClusterRequest> invocation = new HttpOutboundInvocation<>(clusterRequest, context);
        CompletionStage<ReactiveClusterResponse> response = cluster.invoke(invocation);
        CompletableFuture<ClientResponse> future = response.toCompletableFuture().thenApply(ReactiveClusterResponse::getResponse);
        Mono<ClientResponse> mono = Mono.fromFuture(future);
        mc.skipWithResult(mono);
    }

    @Override
    protected String getServiceName(ClientRequest request) {
        return request.url().getHost();
    }

}
