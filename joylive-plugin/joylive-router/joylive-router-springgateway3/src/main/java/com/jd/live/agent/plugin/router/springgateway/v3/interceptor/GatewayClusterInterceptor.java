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
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.InvocationContext.HttpForwardContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.GatewayHttpOutboundInvocation;
import com.jd.live.agent.plugin.router.springgateway.v3.cluster.GatewayCluster;
import com.jd.live.agent.plugin.router.springgateway.v3.config.GatewayConfig;
import com.jd.live.agent.plugin.router.springgateway.v3.request.GatewayClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v3.response.GatewayClusterResponse;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.handler.FilteringWebHandler;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

/**
 * GatewayClusterInterceptor
 *
 * @since 1.0.0
 */
public class GatewayClusterInterceptor extends InterceptorAdaptor {

    private static final String SCHEMA_LB = "lb";
    private static final String TYPE_GATEWAY_FILTER_ADAPTER = "org.springframework.cloud.gateway.handler.FilteringWebHandler$GatewayFilterAdapter";
    private static final String TYPE_REWRITE_PATH_FILTER = "org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory$1";
    private static final String TYPE_RETRY_FILTER = "org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory$1";
    private static final String TYPE_STRIP_PREFIX = "org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory$1";
    private static final String TYPE_ROUTE_TO_REQUEST_URL_FILTER = "org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter";
    private static final String FIELD_RETRY_CONFIG = "val$retryConfig";
    private static final String FIELD_DELEGATE = "delegate";
    private static final String FIELD_CLIENT_FACTORY = "clientFactory";
    private static final String FIELD_GLOBAL_FILTERS = "globalFilters";
    private static final String SCHEME_REGEX = "[a-zA-Z]([a-zA-Z]|\\d|\\+|\\.|-)*:.*";
    private static final Pattern SCHEME_PATTERN = Pattern.compile(SCHEME_REGEX);

    private final InvocationContext context;

    private final GatewayConfig config;

    private final Set<String> pathFilters;

    private final Map<Object, FilterConfig> clusters = new ConcurrentHashMap<>();

    public GatewayClusterInterceptor(InvocationContext context, GatewayConfig config) {
        this.context = context;
        this.config = config;
        this.pathFilters = config == null || config.getPathFilters() == null ? new HashSet<>(2) : new HashSet<>(config.getPathFilters());
        pathFilters.add(TYPE_REWRITE_PATH_FILTER);
        pathFilters.add(TYPE_STRIP_PREFIX);
    }

    /**
     * Enhanced logic before method execution
     * <p>
     *
     * @param ctx ExecutableContext
     * @see FilteringWebHandler#handle(ServerWebExchange)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        RequestContext.setAttribute(Carrier.ATTRIBUTE_GATEWAY, Boolean.TRUE);

        ServerWebExchange exchange = ctx.getArgument(0);
        FilterConfig filterConfig = clusters.computeIfAbsent(ctx.getTarget(), this::createFilterConfig);
        LiveGatewayFilterChain chain = filterConfig.chain(exchange);
        GatewayCluster cluster = filterConfig.getCluster();
        InvocationContext ic = chain.isLoadBalance() ? context : new HttpForwardContext(context);
        ReactiveLoadBalancer.Factory<ServiceInstance> factory = chain.isLoadBalance() ? cluster.getClientFactory() : null;
        RetryConfig retryConfig = RequestContext.removeAttribute(GatewayConfig.ATTRIBUTE_RETRY_CONFIG);
        GatewayClusterRequest request = new GatewayClusterRequest(exchange, chain, factory, retryConfig, config);
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

        MethodContext mc = (MethodContext) ctx;
        mc.setResult(Mono.fromCompletionStage(result));
        mc.setSkip(true);
    }

    /**
     * Returns the filter configuration for the given target object.
     *
     * @param target The target object.
     * @return The filter configuration.
     */
    private FilterConfig createFilterConfig(Object target) {
        List<GatewayFilter> globalFilters = getGlobalFilters(target);
        List<GatewayFilter> filters = new ArrayList<>(globalFilters.size());
        LoadBalancerClientFactory factory = null;
        for (GatewayFilter filter : globalFilters) {
            if (filter instanceof OrderedGatewayFilter) {
                filter = ((OrderedGatewayFilter) filter).getDelegate();
            }
            String filterClassName = filter.getClass().getName();
            if (filterClassName.equals(TYPE_GATEWAY_FILTER_ADAPTER)) {
                GlobalFilter globalFilter = getGlobalFilter(filter);
                if (globalFilter instanceof ReactiveLoadBalancerClientFilter) {
                    // skip ReactiveLoadBalancerClientFilter, because it's implement by RouteFilter
                    factory = getLoadBalancerClientFactory(globalFilter);
                } else if (!globalFilter.getClass().getName().equals(TYPE_ROUTE_TO_REQUEST_URL_FILTER)) {
                    // the filter is implemented by parseURI
                    filters.add(filter);
                }
            }
        }

        return new FilterConfig(target, filters, pathFilters, new GatewayCluster(factory));
    }

    /**
     * Returns the global filter associated with the given gateway filter.
     *
     * @param filter The gateway filter.
     * @return The global filter.
     */
    private GlobalFilter getGlobalFilter(GatewayFilter filter) {
        return (GlobalFilter) ClassUtils
                .describe(filter.getClass())
                .getFieldList()
                .getField(FIELD_DELEGATE)
                .get(filter);
    }

    /**
     * Returns the load balancer client factory associated with the given global filter.
     *
     * @param globalFilter The global filter.
     * @return The load balancer client factory.
     */
    private LoadBalancerClientFactory getLoadBalancerClientFactory(GlobalFilter globalFilter) {
        return (LoadBalancerClientFactory) ClassUtils
                .describe(globalFilter.getClass())
                .getFieldList()
                .getField(FIELD_CLIENT_FACTORY)
                .get(globalFilter);
    }

    /**
     * Returns the list of global gateway filters associated with the given target object.
     *
     * @param target The target object.
     * @return The list of gateway filters.
     */
    @SuppressWarnings("unchecked")
    private List<GatewayFilter> getGlobalFilters(Object target) {
        return (List<GatewayFilter>) ClassUtils
                .describe(target.getClass())
                .getFieldList()
                .getField(FIELD_GLOBAL_FILTERS)
                .get(target);
    }

    /**
     * A live gateway filter chain that allows dynamic addition and removal of filters.
     */
    private static class LiveGatewayFilterChain implements GatewayFilterChain {

        protected static final GatewayFilterChain EMPTY_CHAIN = new LiveGatewayFilterChain(Collections.emptyList());

        private final List<GatewayFilter> filters;

        @Getter
        private final boolean loadBalance;

        private int index;

        LiveGatewayFilterChain(List<GatewayFilter> filters) {
            this(filters, false);
        }

        LiveGatewayFilterChain(List<GatewayFilter> filters, boolean loadBalance) {
            this.filters = filters;
            this.loadBalance = loadBalance;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            final int idx = index++;
            return Mono.defer(() -> {
                if (idx < filters.size()) {
                    GatewayFilter filter = filters.get(idx);
                    return filter.filter(exchange, this);
                } else {
                    return Mono.empty(); // complete
                }
            });
        }

    }

    /**
     * A utility class that holds the configuration for gateway filters.
     */
    @Getter
    private static class FilterConfig {

        private final Object target;

        private final List<GatewayFilter> globalFilters;

        private final Set<String> pathFilters;

        private final GatewayCluster cluster;

        private Optional<FieldDesc> retryOption;

        FilterConfig(Object target, List<GatewayFilter> globalFilters, Set<String> pathFilters, GatewayCluster cluster) {
            this.target = target;
            this.globalFilters = globalFilters;
            this.pathFilters = pathFilters;
            this.cluster = cluster;
        }

        /**
         * Creates a new LiveGatewayFilterChain instance based on the given ServerWebExchange object and Route.
         *
         * @param exchange The ServerWebExchange object, which contains information about the request and response.
         * @return A new LiveGatewayFilterChain instance.
         */
        public LiveGatewayFilterChain chain(ServerWebExchange exchange) {
            Route route = exchange.getRequiredAttribute(GATEWAY_ROUTE_ATTR);
            List<GatewayFilter> filters = globalFilters;
            List<GatewayFilter> pathFilters = new ArrayList<>(4);
            GatewayFilter delegate;
            for (GatewayFilter filter : route.getFilters()) {
                delegate = filter instanceof OrderedGatewayFilter ? ((OrderedGatewayFilter) filter).getDelegate() : null;
                if (delegate != null && delegate.getClass().getName().equals(TYPE_RETRY_FILTER)) {
                    onRetryFilter(delegate);
                } else if (delegate != null && isPathFilter(delegate)) {
                    pathFilters.add(delegate);
                } else {
                    if (filters == globalFilters) {
                        filters = new ArrayList<>(getGlobalFilters());
                    }
                    filters.add(filter);
                }
            }
            AnnotationAwareOrderComparator.sort(filters);
            AnnotationAwareOrderComparator.sort(pathFilters);
            return new LiveGatewayFilterChain(filters, pareURI(exchange, route, pathFilters));
        }

        /**
         * Checks if the given GatewayFilter is a path filter.
         *
         * @param filter The GatewayFilter to check.
         * @return true if the filter is a path filter, false otherwise.
         */
        private boolean isPathFilter(GatewayFilter filter) {
            return pathFilters.contains(filter.getClass().getName());
        }

        /**
         * Handles the retry filter by setting the retry configuration in the request context.
         *
         * @param filter The GatewayFilter instance to process.
         */
        private void onRetryFilter(GatewayFilter filter) {
            if (retryOption == null) {
                retryOption = Optional.ofNullable(ClassUtils.describe(filter.getClass()).getFieldList().getField(FIELD_RETRY_CONFIG));
            }
            retryOption.ifPresent(f -> {
                RetryConfig retryConfig = (RetryConfig) f.get(filter);
                RequestContext.setAttribute(GatewayConfig.ATTRIBUTE_RETRY_CONFIG, retryConfig);
            });
        }

        /**
         * Parses the URI and determines whether load balancing is used based on the given route information, the current ServerWebExchange object, and an optional list of GatewayFilters for rewriting the path.
         *
         * @param exchange          The current ServerWebExchange object, which contains information about the request and response.
         * @param route             The current route information, including the path, host, port, etc.
         * @param pathFilters       An optional list of GatewayFilters used to rewrite the path.
         * @return A boolean indicating whether load balancing is used.
         */
        private boolean pareURI(ServerWebExchange exchange, Route route, List<GatewayFilter> pathFilters) {
            URI routeUri = route.getUri();

            String scheme = routeUri.getScheme();
            String schemePrefix = null;
            boolean hasAnotherScheme = routeUri.getHost() == null && routeUri.getRawPath() == null
                    && SCHEME_PATTERN.matcher(routeUri.getSchemeSpecificPart()).matches();
            Map<String, Object> attributes = exchange.getAttributes();
            if (hasAnotherScheme) {
                schemePrefix = routeUri.getScheme();
                attributes.put(GATEWAY_SCHEME_PREFIX_ATTR, schemePrefix);
                routeUri = URI.create(routeUri.getSchemeSpecificPart());
                scheme = routeUri.getScheme();
            }

            if (pathFilters != null) {
                for (GatewayFilter filter : pathFilters) {
                    filter.filter(exchange, LiveGatewayFilterChain.EMPTY_CHAIN);
                }
            }
            URI uri = exchange.getAttributeOrDefault(GATEWAY_REQUEST_URL_ATTR, exchange.getRequest().getURI());
            boolean encoded = containsEncodedParts(uri);
            uri = UriComponentsBuilder.fromUri(uri)
                    .scheme(routeUri.getScheme())
                    .host(routeUri.getHost())
                    .port(routeUri.getPort())
                    .build(encoded)
                    .toUri();
            attributes.put(GATEWAY_REQUEST_URL_ATTR, uri);
            return SCHEMA_LB.equals(scheme) || SCHEMA_LB.equals(schemePrefix);
        }

    }
}
