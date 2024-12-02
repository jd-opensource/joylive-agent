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
package com.jd.live.agent.plugin.router.springgateway.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.InvocationContext.HttpForwardContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.GatewayHttpOutboundInvocation;
import com.jd.live.agent.plugin.router.springgateway.v2.cluster.GatewayCluster;
import com.jd.live.agent.plugin.router.springgateway.v2.config.GatewayConfig;
import com.jd.live.agent.plugin.router.springgateway.v2.request.GatewayClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v2.response.GatewayClusterResponse;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.core.util.type.ClassUtils.getValue;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR;

/**
 * GatewayClusterInterceptor
 *
 * @since 1.0.0
 */
public class GatewayClusterInterceptor extends InterceptorAdaptor {

    private static final String FIELD_CLIENT_FACTORY = "clientFactory";
    private static final String SCHEMA_LB = "lb";

    private final InvocationContext context;

    private final GatewayConfig config;

    private final Map<Object, GatewayCluster> clusters = new ConcurrentHashMap<>();

    public GatewayClusterInterceptor(InvocationContext context, GatewayConfig config) {
        this.context = context;
        this.config = config;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {

        MethodContext mc = (MethodContext) ctx;

        RequestContext.setAttribute(Carrier.ATTRIBUTE_GATEWAY, Boolean.TRUE);
        ServerWebExchange exchange = ctx.getArgument(0);
        GatewayFilterChain chain = ctx.getArgument(1);
        URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = exchange.getAttribute(GATEWAY_SCHEME_PREFIX_ATTR);
        GatewayCluster cluster = clusters.computeIfAbsent(ctx.getTarget(), t -> new GatewayCluster(getValue(t, FIELD_CLIENT_FACTORY)));
        RetryConfig retryConfig = RequestContext.removeAttribute(GatewayConfig.ATTRIBUTE_RETRY_CONFIG);
        boolean lb = url != null && (GatewayClusterInterceptor.SCHEMA_LB.equals(url.getScheme()) || GatewayClusterInterceptor.SCHEMA_LB.equals(schemePrefix));
        GatewayClusterRequest request = new GatewayClusterRequest(exchange, chain, lb ? cluster.getClientFactory() : null, retryConfig, config);
        InvocationContext ic = lb ? context : new HttpForwardContext(context);
        OutboundInvocation<GatewayClusterRequest> invocation = new GatewayHttpOutboundInvocation<>(request, ic);

        CompletionStage<GatewayClusterResponse> response = cluster.invoke(invocation);
        CompletableFuture<Void> result = new CompletableFuture<>();
        response.whenComplete((v, t) -> {
            if (t != null) {
                result.completeExceptionally(t);
            } else if (v.getError() != null) {
                result.completeExceptionally(v.getError().getThrowable());
            } else {
                result.complete(null);
            }
        });

        mc.skipWithResult(Mono.fromCompletionStage(result));
    }
}
