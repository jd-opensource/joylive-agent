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
import com.jd.live.agent.governance.config.RegistryConfig;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v4.cluster.WebClientCluster;
import com.jd.live.agent.plugin.router.springcloud.v4.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v4.exception.WebClientThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v4.request.ReactiveOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.request.WebClientClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.response.ReactiveClusterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * WebClientClusterInterceptor
 */
public class WebClientClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final Registry registry;

    private final RegistryConfig config;

    private final SpringOutboundThrower<ReactiveOutboundRequest> thrower = new SpringOutboundThrower(WebClientThrowerFactory.INSTANCE);

    public WebClientClusterInterceptor(InvocationContext context, Registry registry) {
        this.context = context;
        this.registry = registry;
        this.config = context.getGovernanceConfig().getRegistryConfig();
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
        try {
            registry.subscribe(service).get(5000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String errorMessage = "Failed to get governance policy for " + service + ", caused by " + cause.getMessage();
            return Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage).build());
        } catch (TimeoutException e) {
            String errorMessage = "Failed to get governance policy for " + service + ", caused by it's timeout.";
            return Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage).build());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMessage = "Failed to execute request, the request is interrupted";
            return Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage).build());
        } catch (Throwable e) {
            return Mono.error(e);
        }
        return null;
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
        ReactiveOutboundRequest ror = new ReactiveOutboundRequest(request, service);
        HttpOutboundInvocation<ReactiveOutboundRequest> invocation = new HttpOutboundInvocation<>(ror, context);
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
        WebClientClusterRequest clusterRequest = new WebClientClusterRequest(request, service, registry, next);
        HttpOutboundInvocation<WebClientClusterRequest> invocation = new HttpOutboundInvocation<>(clusterRequest, context);
        CompletionStage<ReactiveClusterResponse> stage = WebClientCluster.INSTANCE.invoke(invocation);
        return Mono.fromFuture(stage.toCompletableFuture().thenApply(ReactiveClusterResponse::getResponse));
    }
}
