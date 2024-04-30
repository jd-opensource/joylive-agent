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
import com.jd.live.agent.core.util.template.Template;
import com.jd.live.agent.governance.interceptor.AbstractInterceptor.AbstractHttpRouteInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.GatewayHttpOutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.domain.Domain;
import com.jd.live.agent.governance.policy.domain.DomainPolicy;
import com.jd.live.agent.governance.policy.live.*;
import com.jd.live.agent.plugin.router.springgateway.v3.config.GatewayConfig;
import com.jd.live.agent.plugin.router.springgateway.v3.request.ReactiveOutboundRequest;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

/**
 * ReactiveLoadBalancerClientFilterInterceptor
 *
 * @since 1.0.0
 */
public class ReactiveLoadBalancerClientFilterInterceptor extends AbstractHttpRouteInterceptor<ReactiveOutboundRequest> {

    private final GatewayConfig config;

    private final Map<String, Template> templates = new ConcurrentHashMap<>();

    public ReactiveLoadBalancerClientFilterInterceptor(InvocationContext context, List<RouteFilter> filters, GatewayConfig config) {
        super(context, filters);
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
        ServerWebExchange exchange = (ServerWebExchange) mc.getArguments()[0];
        URI uri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = exchange.getAttribute(GATEWAY_SCHEME_PREFIX_ATTR);
        String scheme = uri == null ? null : uri.getScheme();
        if (scheme != null && ("http".equals(scheme) ||
                "http".equals(schemePrefix) ||
                "https".equals(scheme) ||
                "https".equals(schemePrefix))) {
            forwardHttp(exchange, uri);
        }
    }

    @Override
    protected HttpOutboundInvocation<ReactiveOutboundRequest> createOutlet(ReactiveOutboundRequest request) {
        return new GatewayHttpOutboundInvocation<>(request, context);
    }

    private void forwardHttp(ServerWebExchange exchange, URI uri) {
        ReactiveOutboundRequest request = new ReactiveOutboundRequest(exchange.getRequest().mutate().uri(uri).build(), null);
        HttpOutboundInvocation<ReactiveOutboundRequest> invocation = createOutlet(request);
        routing(invocation);
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

    private String getUnitHost(HttpOutboundInvocation<ReactiveOutboundRequest> invocation, Unit unit) {
        ReactiveOutboundRequest request = invocation.getRequest();
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
