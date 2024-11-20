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
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.plugin.router.springcloud.v4.cluster.ReactiveCluster;
import com.jd.live.agent.plugin.router.springcloud.v4.request.ReactiveClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.response.ReactiveClusterResponse;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
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
public class ReactiveClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final Map<LoadBalancedExchangeFilterFunction, ReactiveCluster> clusters = new ConcurrentHashMap<>();

    public ReactiveClusterInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = ctx.getArguments();
        ReactiveCluster cluster = clusters.computeIfAbsent((LoadBalancedExchangeFilterFunction) ctx.getTarget(), ReactiveCluster::new);
        ReactiveClusterRequest request = new ReactiveClusterRequest((ClientRequest) arguments[0],
                cluster.getLoadBalancerFactory(), (ExchangeFunction) arguments[1]);
        HttpOutboundInvocation<ReactiveClusterRequest> invocation = new HttpOutboundInvocation<>(request, context);
        CompletionStage<ReactiveClusterResponse> response = cluster.invoke(invocation);
        CompletableFuture<ClientResponse> future = response.toCompletableFuture().thenApply(ReactiveClusterResponse::getResponse);
        Mono<ClientResponse> mono = Mono.fromFuture(future);
        mc.setResult(mono);
        mc.setSkip(true);
    }
}
