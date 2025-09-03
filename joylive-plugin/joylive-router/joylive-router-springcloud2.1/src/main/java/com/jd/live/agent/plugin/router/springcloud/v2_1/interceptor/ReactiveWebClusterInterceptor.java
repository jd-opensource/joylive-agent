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

import com.jd.live.agent.bootstrap.bytekit.context.ConstructorContext;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v2_1.cluster.ReactiveWebCluster;
import com.jd.live.agent.plugin.router.springcloud.v2_1.request.ReactiveCloudOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v2_1.request.ReactiveWebClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v2_1.response.ReactiveClusterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * ReactiveWebClusterInterceptor
 */
public class ReactiveWebClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final Registry registry;

    public ReactiveWebClusterInterceptor(InvocationContext context, Registry registry) {
        this.context = context;
        this.registry = registry;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        ConstructorContext cc = (ConstructorContext) ctx;
        Object[] arguments = cc.getArguments();
        if (arguments != null && arguments.length > 0 && arguments[0] instanceof ExchangeFunction) {
            arguments[0] = ((ExchangeFunction) arguments[0]).filter((request, next) -> {
                String service = context.getService(request.url());
                try {
                    List<ServiceEndpoint> endpoints = registry.subscribeAndGet(service, 5000, (message, e) -> e);
                    if (endpoints == null || endpoints.isEmpty()) {
                        // Failed to convert microservice, fallback to domain reques
                        return next.exchange(request);
                    }
                } catch (Throwable e) {
                    return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage()).build());
                }
                if (context.isFlowControlEnabled()) {
                    return request(request, next, service);
                } else {
                    return route(request, next, service);
                }
            });
        }
    }

    /**
     * Routes request to selected service endpoint
     *
     * @param request Original client request
     * @param next    Exchange function
     * @param service Target service identifier
     * @return Proxied response stream
     */
    private Mono<ClientResponse> route(ClientRequest request, ExchangeFunction next, String service) {
        ReactiveCloudOutboundRequest ror = new ReactiveCloudOutboundRequest(request, service);
        HttpOutboundInvocation<ReactiveCloudOutboundRequest> invocation = new HttpOutboundInvocation<>(ror, context);
        ServiceEndpoint endpoint = context.route(invocation);
        request = ClientRequest.from(request).url(HttpUtils.newURI(request.url(), endpoint.getHost(), endpoint.getPort())).build();
        return next.exchange(request);
    }

    /**
     * Executes load-balanced cluster request
     *
     * @param request Inbound request
     * @param next    Downstream handler
     * @param service Destination service
     * @return Async response pipeline
     */
    private Mono<ClientResponse> request(ClientRequest request, ExchangeFunction next, String service) {
        ReactiveWebClusterRequest clusterRequest = new ReactiveWebClusterRequest(request, service, registry, next);
        HttpOutboundInvocation<ReactiveWebClusterRequest> invocation = new HttpOutboundInvocation<>(clusterRequest, context);
        CompletionStage<ReactiveClusterResponse> stage = ReactiveWebCluster.INSTANCE.invoke(invocation);
        return Mono.fromFuture(stage.toCompletableFuture().thenApply(ReactiveClusterResponse::getResponse));
    }
}
