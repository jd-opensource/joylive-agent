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
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.InvocationContext.HttpForwardContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v3.cluster.ReactiveWebCluster;
import com.jd.live.agent.plugin.router.springcloud.v3.request.ReactiveClientClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.request.ReactiveClientForwardRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.response.ReactiveClusterResponse;
import org.springframework.cloud.client.loadbalancer.reactive.DeferringLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.support.HttpAccessor;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;
import static com.jd.live.agent.plugin.router.springcloud.v3.condition.ConditionalOnSpringCloud3Enabled.TYPE_HINT_REQUEST_CONTEXT;

/**
 * ReactiveClientInterceptor
 */
public class ReactiveClientInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final Registry registry;

    public ReactiveClientInterceptor(InvocationContext context) {
        this.context = context;
        this.registry = context.getRegistry();
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        ExchangeFunction exchanger = ctx.getArgument(0);
        WebClient.Builder builder = ctx.getArgument(5);
        if (Accessor.isCloudEnabled()) {
            // with spring cloud
            if (!Accessor.isCloudClient(builder) && context.isSubdomainEnabled()) {
                // Handle multi-active and lane domains
                ctx.setArgument(0, exchanger.filter(this::forward));
            }
        } else if (context.isMicroserviceTransformEnabled()) {
            // Convert regular spring web requests to microservice calls
            ctx.setArgument(0, exchanger.filter(this::invoke));
        } else if (context.isSubdomainEnabled()) {
            // Handle multi-active and lane domains
            ctx.setArgument(0, exchanger.filter(this::forward));
        }
    }

    /**
     * Forwards the request with potential URI transformation.
     *
     * @param request the client request to forward
     * @param next    the next exchange function in the chain
     * @return the response mono
     */
    private Mono<ClientResponse> forward(ClientRequest request, ExchangeFunction next) {
        if (context.isSubdomainEnabled(request.url().getHost())) {
            URI newUri = HttpForwardContext.of(context).route(new ReactiveClientForwardRequest(request));
            if (newUri != request.url()) {
                return next.exchange(ClientRequest.from(request).url(newUri).build());
            }
        }
        return next.exchange(request);
    }

    /**
     * Converts domain-based request to microservice invocation.
     * If service discovery fails, falls back to domain forwarding.
     *
     * @param request the client request
     * @param next the exchange function for fallback
     * @return Mono containing the client response
     */
    private Mono<ClientResponse> invoke(ClientRequest request, ExchangeFunction next) {
        String service = context.getService(request.url());
        if (service == null || service.isEmpty()) {
            // Handle multi-active and lane domains
            return forward(request, next);
        }
        try {
            List<ServiceEndpoint> endpoints = registry.subscribeAndGet(service, 5000, (message, e) -> e);
            if (endpoints == null || endpoints.isEmpty()) {
                // Failed to convert microservice, fallback to domain request
                return next.exchange(request);
            }
        } catch (Throwable e) {
            return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage()).build());
        }
        ReactiveClientClusterRequest req = new ReactiveClientClusterRequest(request, service, registry, next);
        HttpOutboundInvocation<ReactiveClientClusterRequest> invocation = new HttpOutboundInvocation<>(req, context);
        CompletionStage<ReactiveClusterResponse> stage = ReactiveWebCluster.INSTANCE.invoke(invocation);
        return Mono.fromFuture(stage.toCompletableFuture().thenApply(ReactiveClusterResponse::getResponse));
    }

    /**
     * Utility class for detecting Spring Cloud environment and load balancer configuration.
     */
    private static class Accessor {

        // spring cloud 3+
        private static final Class<?> lbType = loadClass(TYPE_HINT_REQUEST_CONTEXT, HttpAccessor.class.getClassLoader());

        /**
         * Checks if Spring Cloud is available in the classpath.
         *
         * @return true if Spring Cloud is present, false otherwise
         */
        public static boolean isCloudEnabled() {
            return lbType != null;
        }

        /**
         * Checks if the WebClient builder is configured for cloud load balancing.
         *
         * @param builder the WebClient builder to check
         * @return true if the builder contains load balancing filters, false otherwise
         */
        public static boolean isCloudClient(WebClient.Builder builder) {
            final boolean[] result = new boolean[]{false};
            builder.filters(filters -> {
                for (ExchangeFilterFunction filter : filters) {
                    if (filter instanceof LoadBalancedExchangeFilterFunction || filter instanceof DeferringLoadBalancerExchangeFilterFunction) {
                        result[0] = true;
                        break;
                    }
                }
            });
            return result[0];
        }
    }
}
