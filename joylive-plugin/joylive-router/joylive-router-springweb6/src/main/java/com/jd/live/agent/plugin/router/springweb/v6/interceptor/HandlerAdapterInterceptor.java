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
package com.jd.live.agent.plugin.router.springweb.v6.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.InboundInvocation.GatewayInboundInvocation;
import com.jd.live.agent.governance.invoke.InboundInvocation.HttpInboundInvocation;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springweb.v6.request.ReactiveInboundRequest;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.jd.live.agent.plugin.router.springweb.v6.request.ReactiveInboundRequest.KEY_LIVE_REQUEST;

/**
 * Interceptor for reactive request calls to perform traffic governance and exception conversion.
 *
 * <p>This interceptor intercepts Spring WebFlux handler adapter calls, applies traffic control
 * policies on inbound reactive requests, and handles exception conversion for proper error responses.
 *
 */
public class HandlerAdapterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public HandlerAdapterInterceptor(InvocationContext context) {
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        // private Mono<Void> handleRequestWith(ServerWebExchange exchange, Object handler)
        MethodContext mc = (MethodContext) ctx;
        ServerWebExchange exchange = mc.getArgument(0);
        Object handler = mc.getArgument(1);
        ReactiveInboundRequest request = new ReactiveInboundRequest(exchange, handler, context.getGovernanceConfig());
        if (!request.isSystem()) {
            exchange.getAttributes().put(KEY_LIVE_REQUEST, Boolean.TRUE);
            InboundInvocation<ReactiveInboundRequest> invocation = context.getApplication().getService().isGateway()
                    ? new GatewayInboundInvocation<>(request, context)
                    : new HttpInboundInvocation<>(request, context);
            Mono<HandlerResult> mono = context.inbound(invocation, () -> ((Mono<HandlerResult>) mc.invokeOrigin()).toFuture(), request::convert);
            mc.skipWithResult(mono);
        }
    }
}
