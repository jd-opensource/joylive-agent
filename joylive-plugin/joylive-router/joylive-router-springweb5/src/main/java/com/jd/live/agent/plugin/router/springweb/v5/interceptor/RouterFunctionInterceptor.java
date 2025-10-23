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
import com.jd.live.agent.bootstrap.bytekit.context.LockContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.parser.JsonPathParser;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.ExceptionUtils;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.McpConfig;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.invoke.InboundInvocation.HttpInboundInvocation;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.jsonrpc.JsonRpcResponse;
import com.jd.live.agent.plugin.router.springweb.v5.request.ServletInboundRequest;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Optional;

/**
 * Interceptor for RouterFunction's HandlerFunction to provide service governance capabilities.
 *
 * This interceptor wraps the original HandlerFunction to:
 * 1. Handle system and MCP requests differently
 * 2. Apply governance policies for non-system requests
 * 3. Process errors according to MCP protocol if needed
 *
 * The interception happens in a thread-safe manner using locks.
 */
public class RouterFunctionInterceptor extends InterceptorAdaptor {

    private static final LockContext lock = new LockContext.DefaultLockContext();

    private final InvocationContext context;

    private final JsonPathParser parser;

    public RouterFunctionInterceptor(InvocationContext context, JsonPathParser parser) {
        this.context = context;
        this.parser = parser;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        ctx.tryLock(lock);
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        if (ctx.isLocked()) {
            delegate((MethodContext) ctx);
        }
    }

    @Override
    public void onExit(ExecutableContext ctx) {
        ctx.unlock();
    }

    private void delegate(MethodContext ctx) {
        Optional<HandlerFunction<? extends ServerResponse>> optional = ctx.getResult();
        HandlerFunction<? extends ServerResponse> delegate = optional.orElse(null);
        if (delegate == null) {
            return;
        }
        ctx.skipWithResult(Optional.of((HandlerFunction) req -> {
            GovernanceConfig govnConfig = context.getGovernanceConfig();
            McpConfig mcpConfig = govnConfig.getMcpConfig();
            ServiceConfig serviceConfig = govnConfig.getServiceConfig();
            ServletInboundRequest request = new ServletInboundRequest(req.servletRequest(), null, serviceConfig::isSystem, mcpConfig::isMcp, parser);
            if (!request.isSystem()) {
                HttpInboundInvocation<ServletInboundRequest> invocation = new HttpInboundInvocation<>(request, context);
                try {
                    return (ServerResponse) context.inward(invocation, () -> delegate.handle(req));
                } catch (Throwable e) {
                    if (request.isMcp()) {
                        return ServerResponse.ok().body(JsonRpcResponse.createServerErrorResponse(request.getMcpRequestId(), e.getMessage()));
                    }
                    throw ExceptionUtils.toException(e);
                }
            } else {
                return delegate.handle(req);
            }
        }));
    }
}
