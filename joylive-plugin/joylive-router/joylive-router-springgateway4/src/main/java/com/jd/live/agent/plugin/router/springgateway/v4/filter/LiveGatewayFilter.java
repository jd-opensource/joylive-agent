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
package com.jd.live.agent.plugin.router.springgateway.v4.filter;

import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.plugin.router.springgateway.v4.cluster.GatewayCluster;
import com.jd.live.agent.plugin.router.springgateway.v4.config.GatewayConfig;
import com.jd.live.agent.plugin.router.springgateway.v4.request.GatewayClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v4.response.GatewayClusterResponse;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * A private static class that implements the GatewayFilter interface.
 * This class is responsible for handling the filtering logic for the gateway.
 */
public class LiveGatewayFilter implements GatewayFilter {

    /**
     * The invocation context for this filter.
     */
    private final InvocationContext context;

    /**
     * The gateway configuration for this filter.
     */
    private final GatewayConfig gatewayConfig;

    /**
     * The gateway cluster for this filter.
     */
    private final GatewayCluster cluster;

    /**
     * The retry configuration for this filter.
     */
    private final RetryGatewayFilterFactory.RetryConfig retryConfig;

    /**
     * The index of this filter in the chain.
     */
    private final int index;

    /**
     * Constructs a new LiveFilter instance with the specified parameters.
     *
     * @param context       the invocation context for this filter
     * @param gatewayConfig the gateway configuration for this filter
     * @param cluster       the gateway cluster for this filter
     * @param retryConfig   the retry configuration for this filter
     * @param index         the index of this filter in the chain
     */
    public LiveGatewayFilter(InvocationContext context,
                             GatewayConfig gatewayConfig,
                             GatewayCluster cluster,
                             RetryGatewayFilterFactory.RetryConfig retryConfig,
                             int index) {
        this.context = context;
        this.gatewayConfig = gatewayConfig;
        this.cluster = cluster;
        this.retryConfig = retryConfig;
        this.index = index;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return chain.filter(exchange);
        }
        OutboundInvocation<GatewayClusterRequest> invocation = createInvocation(exchange, chain);
        CompletionStage<GatewayClusterResponse> response = cluster.invoke(invocation);
        CompletableFuture<Void> result = new CompletableFuture<>();
        response.whenComplete((v, t) -> {
            if (t != null) {
                result.completeExceptionally(t);
            } else {
                ServiceError error = v.getError();
                if (error != null && error.getThrowable() != null) {
                    result.completeExceptionally(error.getThrowable());
                } else {
                    result.complete(null);
                }
            }
        });
        return Mono.fromCompletionStage(result);
    }

    /**
     * Creates a new OutboundInvocation instance for the specified ServerWebExchange and GatewayFilterChain.
     *
     * @param exchange the ServerWebExchange representing the incoming request
     * @param chain    the GatewayFilterChain representing the remaining filters in the chain
     * @return a new OutboundInvocation instance
     */
    private OutboundInvocation<GatewayClusterRequest> createInvocation(ServerWebExchange exchange,
                                                                       GatewayFilterChain chain) {
        boolean loadbalancer = ((LiveGatewayFilterChain) chain).isLoadbalancer();
        ReactiveLoadBalancer.Factory<ServiceInstance> factory = loadbalancer ? cluster.getClientFactory() : null;
        GatewayClusterRequest request = new GatewayClusterRequest(exchange, factory, chain, gatewayConfig, retryConfig, index);
        InvocationContext ic = loadbalancer ? context : new InvocationContext.HttpForwardContext(context);
        return new OutboundInvocation.GatewayHttpOutboundInvocation<>(request, ic);
    }

}
