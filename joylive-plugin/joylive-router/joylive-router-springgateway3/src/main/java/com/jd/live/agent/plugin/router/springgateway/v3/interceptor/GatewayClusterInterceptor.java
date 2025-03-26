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
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springgateway.v3.config.GatewayConfig;
import com.jd.live.agent.plugin.router.springgateway.v3.filter.LiveChainBuilder;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GatewayClusterInterceptor
 *
 * @since 1.0.0
 */
public class GatewayClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final GatewayConfig config;

    private final Map<Object, LiveChainBuilder> filterConfigs = new ConcurrentHashMap<>();

    public GatewayClusterInterceptor(InvocationContext context, GatewayConfig config) {
        this.context = context;
        this.config = config;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        RequestContext.setAttribute(Carrier.ATTRIBUTE_GATEWAY, Boolean.TRUE);

        Object target = ctx.getTarget();
        ServerWebExchange exchange = ctx.getArgument(0);

        LiveChainBuilder builder = filterConfigs.computeIfAbsent(target, t -> new LiveChainBuilder(context, config, t));
        GatewayFilterChain chain = builder.chain(exchange);

        MethodContext mc = (MethodContext) ctx;
        mc.skipWithResult(chain.filter(exchange));
    }

}
