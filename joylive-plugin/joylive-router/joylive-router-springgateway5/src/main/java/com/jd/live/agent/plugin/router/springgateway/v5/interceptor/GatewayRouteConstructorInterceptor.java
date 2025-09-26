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
package com.jd.live.agent.plugin.router.springgateway.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.gateway.GatewayRouteDefSupplier;
import com.jd.live.agent.governance.invoke.gateway.GatewayRoutes;
import com.jd.live.agent.plugin.router.springgateway.v5.filter.LiveRoutePredicate;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.function.Supplier;

/**
 * GatewayRouteConstructorInterceptor
 */
public class GatewayRouteConstructorInterceptor extends InterceptorAdaptor {

    @Override
    public void onEnter(ExecutableContext ctx) {
        URI uri = ctx.getArgument(1);
        AsyncPredicate<ServerWebExchange> predicate = ctx.getArgument(3);
        if (!(predicate instanceof GatewayRouteDefSupplier)) {
            Object definition = predicate instanceof Supplier ? ((Supplier<?>) predicate).get() : null;
            ctx.setArgument(3, new LiveRoutePredicate(predicate, definition, uri, GatewayRoutes.getVersion()));
        }
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        Route route = (Route) ctx.getTarget();
        GatewayRoutes.update(route, route.getId(), route.getPredicate());
    }
}
