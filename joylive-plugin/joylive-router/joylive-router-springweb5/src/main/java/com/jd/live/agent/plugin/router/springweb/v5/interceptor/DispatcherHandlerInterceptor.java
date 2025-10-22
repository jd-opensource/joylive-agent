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
package com.jd.live.agent.plugin.router.springweb.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.parser.JsonPathParser;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.McpConfig;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.InboundInvocation.GatewayInboundInvocation;
import com.jd.live.agent.governance.invoke.InboundInvocation.HttpInboundInvocation;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springweb.v5.request.ReactiveInboundRequest;
import com.jd.live.agent.plugin.router.springweb.v5.util.CloudUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getQuietly;
import static com.jd.live.agent.governance.util.ResponseUtils.labelHeaders;

/**
 * DispatcherHandlerInterceptor
 */
public class DispatcherHandlerInterceptor extends InterceptorAdaptor {

    private static final String FIELD_EXCEPTION_HANDLER = "exceptionHandler";

    private final InvocationContext context;

    private final JsonPathParser parser;

    public DispatcherHandlerInterceptor(InvocationContext context, JsonPathParser parser) {
        this.context = context;
        this.parser = parser;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        // private Mono<HandlerResult> invokeHandler(ServerWebExchange exchange, Object handler)
        GovernanceConfig govnConfig = context.getGovernanceConfig();
        McpConfig mcpConfig = govnConfig.getMcpConfig();
        ServiceConfig serviceConfig = govnConfig.getServiceConfig();
        MethodContext mc = (MethodContext) ctx;
        ServerWebExchange exchange = (ServerWebExchange) mc.getArguments()[0];
        Object handler = mc.getArguments()[1];
        ReactiveInboundRequest request = new ReactiveInboundRequest(exchange.getRequest(), handler, serviceConfig::isSystem, mcpConfig::isMcp, parser);
        if (!request.isSystem()) {
            InboundInvocation<ReactiveInboundRequest> invocation = context.getApplication().getService().isGateway()
                    ? new GatewayInboundInvocation<>(request, context)
                    : new HttpInboundInvocation<>(request, context);
            Mono<HandlerResult> mono = context.inbound(invocation, () -> ((Mono<HandlerResult>) mc.invokeOrigin()).toFuture(), request::convert);
            if (serviceConfig.isResponseException()) {
                mono = mono.doOnError(ex -> {
                    HttpHeaders headers = CloudUtils.writable(exchange.getResponse().getHeaders());
                    labelHeaders(ex, headers::set);
                }).doOnSuccess(result -> {
                    if (result != null) {
                        Function<Throwable, Mono<HandlerResult>> exceptionHandler = getQuietly(result, FIELD_EXCEPTION_HANDLER);
                        result.setExceptionHandler(ex -> {
                            HttpHeaders headers = CloudUtils.writable(exchange.getResponse().getHeaders());
                            labelHeaders(ex, headers::set);
                            return exceptionHandler != null ? exceptionHandler.apply(ex) : Mono.error(ex);
                        });
                    }
                });
            }
            mc.skipWithResult(mono);
        }
    }
}
