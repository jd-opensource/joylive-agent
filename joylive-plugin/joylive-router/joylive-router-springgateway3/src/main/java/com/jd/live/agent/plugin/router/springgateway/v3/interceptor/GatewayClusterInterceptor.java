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
package com.jd.live.agent.plugin.router.springgateway.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.InvocationContext.HttpForwardContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.GatewayHttpOutboundInvocation;
import com.jd.live.agent.plugin.router.springgateway.v3.cluster.GatewayCluster;
import com.jd.live.agent.plugin.router.springgateway.v3.config.GatewayConfig;
import com.jd.live.agent.plugin.router.springgateway.v3.request.GatewayClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v3.response.GatewayClusterResponse;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR;

/**
 * ReactiveLoadBalancerClientFilterInterceptor
 *
 * @since 1.0.0
 */
public class GatewayClusterInterceptor extends InterceptorAdaptor {

    public static final String SCHEMA_LB = "lb";

    private final InvocationContext context;

    private final GatewayConfig config;

    private final Map<GlobalFilter, GatewayCluster> clusters = new ConcurrentHashMap<>();

    public GatewayClusterInterceptor(InvocationContext context, GatewayConfig config) {
        this.context = context;
        this.config = config;
    }

    /**
     * Enhanced logic before method execution
     * <p>
     *
     * @param ctx ExecutableContext
     * @see ReactiveLoadBalancerClientFilter#filter(ServerWebExchange, GatewayFilterChain)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = mc.getArguments();
        ServerWebExchange exchange = (ServerWebExchange) arguments[0];
        GatewayFilterChain chain = (GatewayFilterChain) arguments[1];

        RetryGatewayFilterFactory.RetryConfig retryConfig = RequestContext.removeAttribute(GatewayConfig.ATTRIBUTE_RETRY_CONFIG);
        GatewayCluster cluster = clusters.computeIfAbsent((GlobalFilter) ctx.getTarget(), GatewayCluster::new);
        URI uri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = exchange.getAttribute(GATEWAY_SCHEME_PREFIX_ATTR);
        String scheme = uri == null ? null : uri.getScheme();
        boolean loadBalance = SCHEMA_LB.equals(scheme) || SCHEMA_LB.equals(schemePrefix);
        InvocationContext ic = loadBalance ? context : new HttpForwardContext(context);
        ReactiveLoadBalancer.Factory<ServiceInstance> factory = loadBalance ? cluster.getClientFactory() : null;

        GatewayClusterRequest request = new GatewayClusterRequest(exchange, chain, factory, retryConfig, config);
        OutboundInvocation<GatewayClusterRequest> invocation = new GatewayHttpOutboundInvocation<>(request, ic);
        CompletionStage<GatewayClusterResponse> response = cluster.invoke(ic, invocation);
        CompletableFuture<Void> result = new CompletableFuture<>();
        response.whenComplete((v, t) -> {
            if (t != null) {
                result.completeExceptionally(t);
            } else if (v.getThrowable() != null) {
                result.completeExceptionally(v.getThrowable());
            } else {
                result.complete(null);
            }
        });
        mc.setResult(Mono.fromCompletionStage(result));
        mc.setSkip(true);
    }
}
