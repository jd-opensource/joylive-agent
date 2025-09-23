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
package com.jd.live.agent.plugin.router.springgateway.v2_1.filter;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.InvocationContext.HttpForwardContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.GatewayHttpOutboundInvocation;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.request.HostTransformer;
import com.jd.live.agent.plugin.router.springcloud.v2_1.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v2_1.exception.status.StatusThrowerFactory;
import com.jd.live.agent.plugin.router.springgateway.v2_1.cluster.GatewayCluster;
import com.jd.live.agent.plugin.router.springgateway.v2_1.config.GatewayConfig;
import com.jd.live.agent.plugin.router.springgateway.v2_1.request.GatewayCloudClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v2_1.request.GatewayForwardRequest;
import com.jd.live.agent.plugin.router.springgateway.v2_1.response.GatewayClusterResponse;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;
import static com.jd.live.agent.plugin.router.springgateway.v2_1.request.GatewayForwardRequest.getURI;
import static com.jd.live.agent.plugin.router.springgateway.v2_1.request.GatewayForwardRequest.setURI;
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
        // Handle multi-active and lane domains
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
        CompletionStage<GatewayClusterResponse> response = cluster.invoke(invocation);
        CompletableFuture<Void> result = new CompletableFuture<>();
        response.whenComplete((v, t) -> {
            if (t != null) {
                result.completeExceptionally(t);
            } else {
                v.completeVoid(result);
            }
        });
        return Mono.fromCompletionStage(result);
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
                    URI newUri = HttpForwardContext.of(context).route(request);
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

            RetryPolicy retryPolicy = new RetryPolicy();
            retryPolicy.setRetry(retryConfig.getRetries());
            retryPolicy.setInterval(Accessor.getInterval(retryConfig));
            retryPolicy.setMethods(new HashSet<>(toList(retryConfig.getMethods(), HttpMethod::name)));
            retryPolicy.setErrorCodes(statuses);
            retryPolicy.setExceptions(new HashSet<>(toList(retryConfig.getExceptions(), Class::getName)));
            return retryPolicy;
        }
        return null;
    }

    private static class Accessor {

        // spring cloud 2.1.3+
        private static final String TYPE_BACK_OFF_CONFIG = "org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory$BackoffConfig";

        private static Class<?> backoffConfig = loadClass(TYPE_BACK_OFF_CONFIG, RetryConfig.class.getClassLoader());

        private static FieldAccessor backOffField = getAccessor(RetryConfig.class, "backoff");

        private static FieldAccessor firstBackoffField = getAccessor(backoffConfig, "firstBackoff");

        public static Long getInterval(RetryConfig retryConfig) {
            Object backoff = backOffField.get(retryConfig);
            Duration firstBackoff = (Duration) firstBackoffField.get(backoff);
            return firstBackoff == null ? null : firstBackoff.toMillis();
        }

    }

}
