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

import com.jd.live.agent.bootstrap.bytekit.context.ConstructorContext;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.config.HostConfig;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v4.cluster.ReactiveWebCluster;
import com.jd.live.agent.plugin.router.springcloud.v4.request.ReactiveCloudOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.request.ReactiveWebClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.response.ReactiveClusterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;

/**
 * WebClientClusterInterceptor
 */
public class ReactiveWebClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final Registry registry;

    private final HostConfig config;

    public ReactiveWebClusterInterceptor(InvocationContext context, Registry registry) {
        this.context = context;
        this.registry = registry;
        this.config = context.getGovernanceConfig().getRegistryConfig().getHostConfig();
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        ConstructorContext cc = (ConstructorContext) ctx;
        Object[] arguments = cc.getArguments();
        if (arguments != null && arguments.length > 0 && arguments[0] instanceof ExchangeFunction) {
            arguments[0] = ((ExchangeFunction) arguments[0]).filter((request, next) -> {
                String service = config.getService(request.url());
                if (service == null || service.isEmpty()) {
                    return next.exchange(request);
                } else {
                    Mono<ClientResponse> error = subscribe(service);
                    if (error != null) {
                        return error;
                    } else if (context.isFlowControlEnabled()) {
                        return request(request, next, service);
                    } else {
                        return route(request, next, service);
                    }
                }
            });
        }
    }

    /**
     * Subscribes to governance policies with timeout handling
     *
     * @param service Target service name
     * @return Response mono or error flow
     */
    private Mono<ClientResponse> subscribe(String service) {
        return registry.subscribe(service, (message, e) ->
                Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).body(message).build()));
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
        ServiceEndpoint endpoint = context.route(invocation, registry.getEndpoints(service));
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
