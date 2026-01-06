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
package com.jd.live.agent.plugin.router.springgateway.v2_2.filter;

import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.InvocationContext.HttpForwardContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.GatewayHttpForwardInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.GatewayHttpOutboundInvocation;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.request.HostTransformer;
import com.jd.live.agent.plugin.router.springcloud.v2_2.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v2_2.exception.status.StatusThrowerFactory;
import com.jd.live.agent.plugin.router.springgateway.v2_2.cluster.GatewayCluster;
import com.jd.live.agent.plugin.router.springgateway.v2_2.config.GatewayConfig;
import com.jd.live.agent.plugin.router.springgateway.v2_2.request.GatewayCloudClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v2_2.request.GatewayForwardRequest;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.plugin.router.springgateway.v2_2.request.GatewayForwardRequest.getURI;
import static com.jd.live.agent.plugin.router.springgateway.v2_2.request.GatewayForwardRequest.setURI;
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
    private final RetryPolicy retryPolicy;

    /**
     * The index of this filter in the chain.
     */
    private final int index;

    private final SpringOutboundThrower<NestedRuntimeException, GatewayForwardRequest> thrower = new SpringOutboundThrower<>(new StatusThrowerFactory<>());


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
                             RetryConfig retryConfig,
                             int index) {
        this.context = context;
        this.gatewayConfig = gatewayConfig;
        this.cluster = cluster;
        this.retryPolicy = getRetryPolicy(retryConfig);
        this.index = index;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return chain.filter(exchange);
        } else if (chain instanceof LiveGatewayFilterChain && ((LiveGatewayFilterChain) chain).isLoadbalancer()) {
            // lb://
            return invoke(exchange, chain);
        }
        return forward(exchange, chain);
    }

    /**
     * Handles the request by creating a {@link GatewayCloudClusterRequest}
     *
     * @param exchange the {@link ServerWebExchange} representing the current HTTP request and response
     * @param chain    the {@link GatewayFilterChain} to proceed with the filter chain
     * @return a {@link Mono} that completes when the request processing is finished, or exceptionally if an error occurs
     */
    private Mono<Void> invoke(ServerWebExchange exchange, GatewayFilterChain chain) {
        GatewayCloudClusterRequest request = new GatewayCloudClusterRequest(exchange, cluster.getContext(), chain, gatewayConfig, retryPolicy, index);
        OutboundInvocation<GatewayCloudClusterRequest> invocation = new GatewayHttpOutboundInvocation<>(request, context);
        CompletionStage<Void> response = cluster.invoke(invocation, r -> null);
        return Mono.fromCompletionStage(response);
    }

    /**
     * Forwards the request by creating a {@link GatewayForwardRequest} and routing it through the context.
     *
     * @param exchange    the {@link ServerWebExchange} representing the current HTTP request and response
     * @param chain       the {@link GatewayFilterChain} to proceed with the filter chain
     * @return a {@link Mono} that completes when the request is forwarded, or emits an error if an exception occurs
     */
    private Mono<Void> forward(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI uri = getURI(exchange);
        if (gatewayConfig.isWebScheme(uri.getScheme())) {
            // not gateway forward scheme, web requests
            HostTransformer transformer = context.getHostTransformer(uri.getHost());
            if (transformer != null) {
                // Handle multi-active and lane domains
                GatewayForwardRequest request = new GatewayForwardRequest(exchange, uri, transformer);
                try {
                    URI newUri = HttpForwardContext.of(context).route(request, new GatewayHttpForwardInvocation(request, context));
                    if (newUri != uri) {
                        setURI(exchange, newUri);
                    }
                } catch (Throwable e) {
                    return Mono.error(thrower.createException(e, request));
                }
            }
        }
        return chain.filter(exchange);
    }

    private static RetryPolicy getRetryPolicy(RetryConfig retryConfig) {
        if (retryConfig != null && retryConfig.getRetries() > 0) {
            Set<String> statuses = new HashSet<>(16);
            retryConfig.getStatuses().forEach(status -> statuses.add(String.valueOf(status.value())));
            Set<HttpStatus.Series> series = new HashSet<>(retryConfig.getSeries());
            if (!series.isEmpty()) {
                for (HttpStatus status : HttpStatus.values()) {
                    if (series.contains(status.series())) {
                        statuses.add(String.valueOf(status.value()));
                    }
                }
            }
            RetryGatewayFilterFactory.BackoffConfig backoff = retryConfig.getBackoff();
            RetryPolicy retryPolicy = new RetryPolicy();
            retryPolicy.setRetry(retryConfig.getRetries());
            retryPolicy.setInterval(backoff != null ? backoff.getFirstBackoff().toMillis() : null);
            retryPolicy.setMethods(new HashSet<>(toList(retryConfig.getMethods(), HttpMethod::name)));
            retryPolicy.setErrorCodes(statuses);
            retryPolicy.setExceptions(new HashSet<>(toList(retryConfig.getExceptions(), Class::getName)));
            return retryPolicy;
        }
        return null;
    }

}
