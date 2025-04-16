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
package com.jd.live.agent.plugin.router.springgateway.v2_1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.router.springgateway.v2_1.filter.LiveRoute;
import com.jd.live.agent.plugin.router.springgateway.v2_1.filter.LiveRoutePredicate;
import com.jd.live.agent.plugin.router.springgateway.v2_1.filter.LiveRoutes;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.Objects;
import java.util.function.Function;

/**
 * GatewayRouteConstructorInterceptor
 *
 * @since 1.7.0
 */
public class GatewayRouteConstructorInterceptor extends InterceptorAdaptor {

    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] args = ctx.getArguments();
        URI uri = (URI) args[1];
        AsyncPredicate<ServerWebExchange> predicate = (AsyncPredicate<ServerWebExchange>) args[3];
        if (!(predicate instanceof LiveRoutePredicate)) {
            args[3] = new LiveRoutePredicate(predicate, uri, LiveRoutes.getVersion());
        }
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        Route route = (Route) ctx.getTarget();
        String routeId = route.getId();
        LiveRoutePredicate predicate = (LiveRoutePredicate) route.getPredicate();
        RouteDefinition definition = predicate.getDefinition();
        Function<String, LiveRoute> routeFunction = id -> new LiveRoute(route, definition, predicate.getVersion());
        LiveRoute oldRoute = LiveRoutes.getOrCreate(routeId, routeFunction);
        if (oldRoute.getRoute() != route
                && predicate.getVersion() != oldRoute.getVersion()
                && (definition == null || !Objects.equals(definition, oldRoute.getDefinition()))) {
            // update cache
            LiveRoutes.put(routeId, routeFunction.apply(routeId));
        }
    }
}
