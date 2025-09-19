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
import com.jd.live.agent.governance.invoke.gateway.GatewayRoute;
import com.jd.live.agent.governance.invoke.gateway.GatewayRoutes;
import com.jd.live.agent.governance.registry.ServiceRegistryFactory;
import com.jd.live.agent.plugin.router.springcloud.v2_2.registry.SpringServiceRegistry;
import com.jd.live.agent.plugin.router.springgateway.v2_2.cluster.GatewayCluster;
import com.jd.live.agent.plugin.router.springgateway.v2_2.cluster.context.GatewayClusterContext;
import com.jd.live.agent.plugin.router.springgateway.v2_2.config.GatewayConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.server.ServerWebExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getQuietly;
import static com.jd.live.agent.plugin.router.springcloud.v2_2.cluster.context.BlockingClusterContext.createFactory;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * A utility class that holds the configuration for gateway filters.
 */
@Getter
public class LiveChainBuilder {

    private static final String TYPE_GATEWAY_FILTER_ADAPTER = "org.springframework.cloud.gateway.handler.FilteringWebHandler$GatewayFilterAdapter";
    private static final String TYPE_RETRY_FILTER = "org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory$1";
    private static final String TYPE_ROUTE_TO_REQUEST_URL_FILTER = "org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter";
    private static final String FIELD_CLIENT_FACTORY = "clientFactory";
    private static final String FIELD_RETRY_CONFIG = "val$retryConfig";
    private static final String FIELD_DELEGATE = "delegate";
    private static final String FIELD_GLOBAL_FILTERS = "globalFilters";
    private static final String FIELD_LOAD_BALANCER = "loadBalancer";
    private static final int WRITE_RESPONSE_FILTER_ORDER = -1;

    /**
     * The invocation context for this filter configuration.
     */
    private final InvocationContext context;

    /**
     * The gateway configuration for this filter configuration.
     */
    private final GatewayConfig gatewayConfig;

    /**
     * The target object for this filter configuration.
     */
    private final Object target;

    /**
     * The list of global filters for this filter configuration.
     */
    private final List<GatewayFilter> globalFilters;

    /**
     * The gateway cluster for this filter configuration.
     */
    private final GatewayCluster cluster;

    private final Map<String, LiveRouteFilter> routeFilters = new ConcurrentHashMap<>();

    /**
     * Constructs a new FilterConfig instance with the specified parameters.
     *
     * @param context       the invocation context for this filter configuration
     * @param gatewayConfig the gateway configuration for this filter configuration
     * @param target        the target object for this filter configuration
     */
    public LiveChainBuilder(InvocationContext context, GatewayConfig gatewayConfig, Object target) {
        this.context = context;
        this.gatewayConfig = gatewayConfig;
        this.target = target;
        FilterDescriptor descriptor = getGlobalFilters(target);
        this.globalFilters = descriptor.getFilters();
        this.cluster = new GatewayCluster(new GatewayClusterContext(context.getRegistry(), descriptor.getSystem(), context.getPropagation()));
    }

    /**
     * Creates a new GatewayFilterChain instance based on the current filter configuration.
     *
     * @param exchange the ServerWebExchange representing the incoming request
     * @return a new GatewayFilterChain instance
     */
    public GatewayFilterChain chain(ServerWebExchange exchange) {
        Route route = exchange.getRequiredAttribute(GATEWAY_ROUTE_ATTR);
        GatewayRoute<Route> gatewayRoute = GatewayRoutes.get(route.getId());
        LiveRouteFilter filter = gatewayRoute.getOrCreate(this::createRouteFilter);
        return filter.build(exchange);
    }

    /**
     * Creates a new instance of LiveRouteFilter based on the given Route object and version.
     *
     * @param route   the Route object to create the LiveRouteFilter from
     * @param version the version of the LiveRouteFilter
     * @return a new instance of LiveRouteFilter
     */
    private LiveRouteFilter createRouteFilter(Route route, long version) {
        List<GatewayFilter> routeFilters = route.getFilters();
        List<GatewayFilter> pathFilters = new ArrayList<>(4);
        List<GatewayFilter> filters = new ArrayList<>(globalFilters);
        RetryGatewayFilterFactory.RetryConfig retryConfig = null;
        if (!routeFilters.isEmpty()) {
            GatewayFilter delegate;
            for (GatewayFilter filter : routeFilters) {
                delegate = filter instanceof OrderedGatewayFilter ? ((OrderedGatewayFilter) filter).getDelegate() : null;
                if (delegate != null) {
                    String name = delegate.getClass().getName();
                    if (name.equals(TYPE_RETRY_FILTER)) {
                        // ignore retry
                        retryConfig = getQuietly(delegate, FIELD_RETRY_CONFIG);
                    } else if (gatewayConfig.isPathFilter(name)) {
                        // ignore path filter
                        pathFilters.add(filter);
                    } else {
                        filters.add(filter);
                    }
                } else {
                    filters.add(filter);
                }
            }
        }
        AnnotationAwareOrderComparator.sort(filters);
        AnnotationAwareOrderComparator.sort(pathFilters);

        // insert filters
        int pos = 0;
        int order;
        for (int i = 0; i < filters.size(); i++) {
            GatewayFilter gatewayFilter = filters.get(i);
            if (gatewayFilter instanceof Ordered) {
                order = ((Ordered) gatewayFilter).getOrder();
                if (order >= WRITE_RESPONSE_FILTER_ORDER) {
                    pos = i;
                    break;
                }
            }

        }
        LiveGatewayFilter liveFilter = new LiveGatewayFilter(context, gatewayConfig, cluster, retryConfig, pos);
        filters.add(pos, new OrderedGatewayFilter(liveFilter, WRITE_RESPONSE_FILTER_ORDER - 1));
        return new LiveRouteFilter(route, filters, pathFilters, version);
    }

    /**
     * Retrieves the list of global filters from the specified target object.
     *
     * @param target the target object
     * @return the list of global filters
     */
    private FilterDescriptor getGlobalFilters(Object target) {
        // this is sorted by order
        return new FilterDescriptor(getQuietly(target, FIELD_GLOBAL_FILTERS));
    }

    private static class FilterDescriptor {
        @Getter
        private List<GatewayFilter> filters;
        @Getter
        @Setter
        private ServiceRegistryFactory system;

        FilterDescriptor(List<GatewayFilter> filters) {
            this.filters = new ArrayList<>(filters == null ? 0 : filters.size());
            if (filters != null) {
                for (GatewayFilter filter : filters) {
                    parse(filter);
                }
            }
        }

        @SuppressWarnings("deprecation")
        protected void parse(GatewayFilter filter) {
            GatewayFilter delegate = filter;
            if (filter instanceof OrderedGatewayFilter) {
                delegate = ((OrderedGatewayFilter) filter).getDelegate();
            }
            if (delegate.getClass().getName().equals(TYPE_GATEWAY_FILTER_ADAPTER)) {
                GlobalFilter globalFilter = getQuietly(delegate, FIELD_DELEGATE);
                if (globalFilter instanceof ReactiveLoadBalancerClientFilter) {
                    LoadBalancerClientFactory clientFactory = getQuietly(globalFilter, FIELD_CLIENT_FACTORY);
                    system = (service -> new SpringServiceRegistry(service, clientFactory));
                } else if (globalFilter instanceof LoadBalancerClientFilter) {
                    LoadBalancerClient client = getQuietly(globalFilter, FIELD_LOAD_BALANCER);
                    system = (createFactory(client));
                } else if (globalFilter == null || !globalFilter.getClass().getName().equals(TYPE_ROUTE_TO_REQUEST_URL_FILTER)) {
                    // the filter is implemented by parseURI
                    filters.add(filter);
                }
            } else {
                filters.add(filter);
            }
        }
    }
}
