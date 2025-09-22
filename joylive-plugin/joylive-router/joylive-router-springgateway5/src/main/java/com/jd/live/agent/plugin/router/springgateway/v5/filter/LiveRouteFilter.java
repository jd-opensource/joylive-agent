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
package com.jd.live.agent.plugin.router.springgateway.v5.filter;

import com.jd.live.agent.governance.invoke.gateway.GatewayRouteDef;
import com.jd.live.agent.governance.invoke.gateway.GatewayRouteDefSupplier;
import com.jd.live.agent.governance.invoke.gateway.GatewayRouteURI;
import com.jd.live.agent.plugin.router.springgateway.v5.filter.LiveGatewayFilterChain.DefaultGatewayFilterChain;
import lombok.Getter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.http.HttpUtils.newURI;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR;

/**
 * A class representing a live filter for a route.
 */
@Getter
public class LiveRouteFilter {

    /**
     * The route associated with this live filter.
     */
    private final Route route;

    /**
     * The list of gateway filters.
     */
    private final List<GatewayFilter> filters;

    /**
     * The list of path filters.
     */
    private final List<GatewayFilter> paths;

    /**
     * The version of this live filter.
     */
    private final long version;

    private final GatewayRouteURI routeURI;

    /**
     * Creates a new instance of LiveFilter.
     *
     * @param route       the route
     * @param filters     the list of gateway filters
     * @param paths       the list of path filters
     * @param version     the version
     */
    public LiveRouteFilter(Route route, List<GatewayFilter> filters, List<GatewayFilter> paths, long version) {
        this.route = route;
        this.filters = filters;
        this.paths = paths;
        this.version = version;
        AsyncPredicate<ServerWebExchange> predicate = route.getPredicate();
        GatewayRouteDef def = predicate instanceof GatewayRouteDefSupplier ? ((GatewayRouteDefSupplier) predicate).getDefinition() : null;
        this.routeURI = def != null ? def.getUri() : new GatewayRouteURI(route.getUri());
    }

    /**
     * Builds a GatewayFilterChain with rewritten URI and load balancing configuration.
     * Sets scheme prefix, applies path filters, and rewrites the request URI based on route configuration.
     *
     * @param exchange the current ServerWebExchange containing request/response information
     * @return a configured GatewayFilterChain with load balancing settings
     */
    public GatewayFilterChain build(ServerWebExchange exchange) {
        Map<String, Object> attributes = exchange.getAttributes();
        if (routeURI.getSchemePrefix() != null) {
            attributes.put(GATEWAY_SCHEME_PREFIX_ATTR, routeURI.getSchemePrefix());
        }
        // handle path filters
        if (paths != null && !paths.isEmpty()) {
            DefaultGatewayFilterChain.of(paths).filter(exchange).subscribe();
        }
        // original uri
        URI uri = (URI) attributes.getOrDefault(GATEWAY_REQUEST_URL_ATTR, exchange.getRequest().getURI());
        // rewrite uri
        uri = newURI(uri, routeURI.getScheme(), routeURI.getHost(), routeURI.getPort());
        attributes.put(GATEWAY_REQUEST_URL_ATTR, uri);
        return new DefaultGatewayFilterChain(filters, routeURI.isLoadBalancer());
    }
}
