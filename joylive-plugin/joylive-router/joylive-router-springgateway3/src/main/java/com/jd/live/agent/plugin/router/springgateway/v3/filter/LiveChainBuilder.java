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
package com.jd.live.agent.plugin.router.springgateway.v3.filter;

import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.gateway.GatewayRoute;
import com.jd.live.agent.governance.invoke.gateway.GatewayRoutes;
import com.jd.live.agent.plugin.router.springgateway.v3.cluster.GatewayCluster;
import com.jd.live.agent.plugin.router.springgateway.v3.config.GatewayConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getQuietly;
import static com.jd.live.agent.core.util.CollectionUtils.add;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * A utility class that holds the configuration for gateway filters.
 */
@Getter
public class LiveChainBuilder {

    private static final String TYPE_GATEWAY_FILTER_ADAPTER = "org.springframework.cloud.gateway.handler.FilteringWebHandler$GatewayFilterAdapter";
    private static final String TYPE_ROUTE_TO_REQUEST_URL_FILTER = "org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter";
    private static final String FIELD_CLIENT_FACTORY = "clientFactory";
    private static final String FIELD_RETRY_CONFIG = "val$retryConfig";
    private static final String FIELD_DELEGATE = "delegate";
    private static final String FIELD_GLOBAL_FILTERS = "globalFilters";

    private static final Map<Object, LiveChainBuilder> BUILDERS = new ConcurrentHashMap<>();

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

    private final ReactiveLoadBalancer.Factory<ServiceInstance> clientFactory;

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
        this.clientFactory = descriptor.getClientFactory();
        this.cluster = new GatewayCluster(context.getRegistry(), clientFactory);
    }

    /**
     * Creates a new GatewayFilterChain instance based on the current filter configuration.
     *
     * @param exchange the ServerWebExchange representing the incoming request
     * @return a new GatewayFilterChain instance
     */
    public GatewayFilterChain create(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        GatewayRoute<Route> gatewayRoute = GatewayRoutes.get(route.getId());
        LiveRouteFilter filter = gatewayRoute.getOrCreate(this::createRouteFilter);
        return filter.build(exchange);
    }

    /**
     * Filters the server web exchange.
     *
     * @param exchange the server web exchange
     * @return a Mono that indicates when request processing is complete
     */
    public Mono<Void> chain(ServerWebExchange exchange) {
        return create(exchange).filter(exchange);
    }

    /**
     * Gets or creates a LiveChainBuilder for the target.
     *
     * @param target  the target object
     * @param context the invocation context
     * @param config  the gateway config
     * @return LiveChainBuilder instance
     */
    public static LiveChainBuilder getOrCreate(Object target, InvocationContext context, GatewayConfig config) {
        return BUILDERS.computeIfAbsent(target, t -> new LiveChainBuilder(context, config, t));
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
        AtomicReference<RetryConfig> retryConfig = new AtomicReference<>();
        List<GatewayFilter> filters = new ArrayList<>(globalFilters);
        add(routeFilters, filters, filter -> !ignore(filter, retryConfig, pathFilters));

        AnnotationAwareOrderComparator.sort(filters);
        AnnotationAwareOrderComparator.sort(pathFilters);
        addFilter(filters, gatewayConfig.getLiveFilterOrder(), pos -> new LiveGatewayFilter(context, gatewayConfig, cluster, retryConfig.get(), pos));

        return new LiveRouteFilter(route, filters, pathFilters, version);

    }

    /**
     * Adds a new ordered filter to the filter list at the appropriate position.
     *
     * @param filters  the list of gateway filters
     * @param order    the order value for the new filter
     * @param function function to create the filter, receives position as parameter
     */
    private void addFilter(List<GatewayFilter> filters, int order, Function<Integer, GatewayFilter> function) {
        int pos = 0;
        for (int i = 0; i < filters.size(); i++) {
            GatewayFilter gatewayFilter = filters.get(i);
            if (gatewayFilter instanceof Ordered) {
                int ord = ((Ordered) gatewayFilter).getOrder();
                if (ord > order) {
                    pos = i;
                    break;
                }
            }
        }
        filters.add(pos, new OrderedGatewayFilter(function.apply(pos), order));
    }

    /**
     * Checks if the given filter should be ignored and handles retry/path filter extraction.
     *
     * @param filter      the gateway filter to check
     * @param retryConfig atomic reference to store retry configuration if found
     * @param pathFilters list to collect path filters
     * @return true if the filter should be ignored, false otherwise
     */
    private boolean ignore(GatewayFilter filter, AtomicReference<RetryConfig> retryConfig, List<GatewayFilter> pathFilters) {
        GatewayFilter delegate = filter instanceof OrderedGatewayFilter ? ((OrderedGatewayFilter) filter).getDelegate() : filter;
        String name = delegate.getClass().getName();
        if (gatewayConfig.isRetryFilter(name)) {
            // ignore retry
            retryConfig.set(getQuietly(delegate, FIELD_RETRY_CONFIG));
            return true;
        } else if (gatewayConfig.isPathFilter(name)) {
            pathFilters.add(filter);
            return true;
        }
        return false;
    }

    /**
     * Retrieves the list of global filters from the specified target object.
     *
     * @param target the target object
     * @return the list of global filters
     */
    private FilterDescriptor getGlobalFilters(Object target) {
        // this is sorted by order
        return new FilterDescriptor(getQuietly(target, FIELD_GLOBAL_FILTERS), gatewayConfig);
    }

    private static class FilterDescriptor {
        @Getter
        private final List<GatewayFilter> filters;

        private final GatewayConfig config;

        @Getter
        @Setter
        private ReactiveLoadBalancer.Factory<ServiceInstance> clientFactory;

        FilterDescriptor(List<GatewayFilter> filters, GatewayConfig config) {
            this.filters = new ArrayList<>(filters == null ? 0 : filters.size());
            this.config = config;
            if (filters != null) {
                for (GatewayFilter filter : filters) {
                    parse(filter);
                }
            }
        }

        protected void parse(GatewayFilter filter) {
            GatewayFilter delegate = filter;
            if (filter instanceof OrderedGatewayFilter) {
                delegate = ((OrderedGatewayFilter) filter).getDelegate();
            }
            if (delegate.getClass().getName().equals(TYPE_GATEWAY_FILTER_ADAPTER)) {
                GlobalFilter globalFilter = getQuietly(delegate, FIELD_DELEGATE);
                if (globalFilter == null) {
                    filters.add(filter);
                } else {
                    String name = globalFilter.getClass().getName();
                    if (config.isLoadBalancerFilter(name)) {
                        // ignore loadbalance
                        // org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter
                        clientFactory = getQuietly(globalFilter, FIELD_CLIENT_FACTORY);
                    } else if (!name.equals(TYPE_ROUTE_TO_REQUEST_URL_FILTER)) {
                        // ignore RouteToRequestUrlFilter, the filter is implemented by parseURI
                        filters.add(filter);
                    }
                }
            } else {
                filters.add(filter);
            }
        }
    }
}
