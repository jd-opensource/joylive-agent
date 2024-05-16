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
import com.jd.live.agent.core.util.template.Template;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.GatewayHttpOutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.domain.Domain;
import com.jd.live.agent.governance.policy.domain.DomainPolicy;
import com.jd.live.agent.governance.policy.live.*;
import com.jd.live.agent.plugin.router.springgateway.v3.cluster.GatewayCluster;
import com.jd.live.agent.plugin.router.springgateway.v3.config.GatewayConfig;
import com.jd.live.agent.plugin.router.springgateway.v3.request.GatewayClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v3.request.GatewayOutboundRequest;
import com.jd.live.agent.plugin.router.springgateway.v3.response.GatewayClusterResponse;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

/**
 * ReactiveLoadBalancerClientFilterInterceptor
 *
 * @since 1.0.0
 */
public class GatewayClusterInterceptor extends InterceptorAdaptor {

    public static final String SCHEMA_LB = "lb";
    private final InvocationContext context;

    private final GatewayConfig config;

    private final Map<String, Template> templates = new ConcurrentHashMap<>();

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
        URI uri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = exchange.getAttribute(GATEWAY_SCHEME_PREFIX_ATTR);
        String scheme = uri == null ? null : uri.getScheme();
        if (scheme != null && ("http".equals(scheme) || "http".equals(schemePrefix)
                || "https".equals(scheme) || "https".equals(schemePrefix))) {
            // TODO HTTP service
            forwardHttp(exchange, uri);
        } else if (SCHEMA_LB.equals(scheme) || SCHEMA_LB.equals(schemePrefix)) {
            RetryGatewayFilterFactory.RetryConfig retryConfig = RequestContext.removeAttribute(GatewayConfig.ATTRIBUTE_RETRY_CONFIG);
            GatewayCluster cluster = clusters.computeIfAbsent((GlobalFilter) ctx.getTarget(), GatewayCluster::new);
            GatewayClusterRequest request = new GatewayClusterRequest(exchange, chain, cluster.getClientFactory(), retryConfig);
            OutboundInvocation<GatewayClusterRequest> invocation = new HttpOutboundInvocation<>(request, context);
            CompletionStage<GatewayClusterResponse> response = cluster.invoke(context, invocation);
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

    private void forwardHttp(ServerWebExchange exchange, URI uri) {
        GatewayOutboundRequest request = new GatewayOutboundRequest(exchange);
        OutboundInvocation<GatewayOutboundRequest> invocation = new GatewayHttpOutboundInvocation<>(request, context);
        context.route(invocation);
        RouteTarget target = invocation.getRouteTarget();
        UnitRoute unitRoute = target.getUnitRoute();
        CellRoute cellRoute = target.getCellRoute();
        Unit unit = unitRoute.getUnit();
        Cell cell = cellRoute.getCell();
        String unitHost = getUnitHost(invocation, unit);
        if (unitHost == null) {
            unitHost = getConfigHost(exchange, uri, unit);
        }
        if (unitHost != null) {
            Template template = templates.computeIfAbsent(unitHost, v -> new Template(v, 128));
            if (template.getVariables() > 0) {
                Map<String, Object> context = new HashMap<>();
                context.put(GatewayConfig.KEY_UNIT, unit.getHostPrefix());
                context.put(GatewayConfig.KEY_CELL, cell.getHostPrefix());
                context.put(GatewayConfig.KEY_HOST, uri.getHost());
                unitHost = template.evaluate(context);
            }
            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, UriComponentsBuilder.fromUri(uri).host(unitHost).build().toUri());
        }
    }

    private String getConfigHost(ServerWebExchange exchange, URI uri, Unit unit) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        Map<String, Object> metadata = route == null ? null : route.getMetadata();
        String hostExpression = metadata == null ? null : (String) metadata.get(GatewayConfig.KEY_HOST_EXPRESSION);
        return hostExpression == null && config != null ? config.getHostExpression() : hostExpression;
    }

    private String getUnitHost(OutboundInvocation<GatewayOutboundRequest> invocation, Unit unit) {
        GatewayOutboundRequest request = invocation.getRequest();
        GovernancePolicy governancePolicy = invocation.getGovernancePolicy();
        Domain domain = governancePolicy == null ? null : governancePolicy.getDomain(request.getHost());
        DomainPolicy domainPolicy = domain == null ? null : domain.getPolicy();
        if (domainPolicy != null) {
            if (domainPolicy.isUnit()) {
                return domainPolicy.getUnitDomain().getHost();
            } else {
                UnitDomain unitDomain = domainPolicy.getLiveDomain().getUnitDomain(unit.getCode());
                return unitDomain == null ? null : unitDomain.getHost();
            }
        }
        return null;
    }
}
