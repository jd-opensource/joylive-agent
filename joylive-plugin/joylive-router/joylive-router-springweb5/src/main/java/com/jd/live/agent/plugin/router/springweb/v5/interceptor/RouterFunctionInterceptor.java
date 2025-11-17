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
import com.jd.live.agent.governance.invoke.InboundInvocation.HttpInboundInvocation;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.mcp.spec.JsonRpcResponse;
import com.jd.live.agent.plugin.router.springweb.v5.request.ServletInboundRequest;
import com.jd.live.agent.plugin.router.springweb.v5.util.CloudUtils;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Optional;

import static com.jd.live.agent.core.util.ExceptionUtils.getCause;
import static com.jd.live.agent.core.util.ExceptionUtils.toException;
import static com.jd.live.agent.plugin.router.springweb.v5.exception.SpringInboundThrower.THROWER;

/**
 * Interceptor for RouterFunction's HandlerFunction to provide service governance capabilities.
 * This interceptor wraps the original HandlerFunction to:
 * 1. Handle system and MCP requests differently
 * 2. Apply governance policies for non-system requests
 * 3. Process errors according to MCP protocol if needed
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
            MethodContext mc = (MethodContext) ctx;
            Optional<HandlerFunction<? extends ServerResponse>> optional = mc.getResult();
            HandlerFunction<? extends ServerResponse> handler = optional.orElse(null);
            if (handler == null || CloudUtils.isResourceHandlerFunction(handler)) {
                return;
            }
            mc.skipWithResult(Optional.of(wrap(ctx.getTarget(), handler)));
        }
    }

    @Override
    public void onExit(ExecutableContext ctx) {
        ctx.unlock();
    }

    /**
     * Wraps a handler function with middleware for request processing and error handling.
     *
     * @param target  The target object that may contain error handling capabilities
     * @param handler The original handler function to be wrapped
     * @return A wrapped handler function with enhanced request processing and error handling
     */
    @SuppressWarnings("rawtypes")
    private HandlerFunction wrap(Object target, HandlerFunction handler) {
        return req -> {
            ServletInboundRequest request = new ServletInboundRequest(req.servletRequest(), null, null, context.getGovernanceConfig(), parser);
            if (!request.isSystem()) {
                HttpInboundInvocation<ServletInboundRequest> invocation = new HttpInboundInvocation<>(request, context);
                try {
                    return (ServerResponse) context.inward(invocation, () -> handler.handle(req));
                } catch (Throwable e) {
                    if (request.isMcp()) {
                        return ServerResponse.ok().body(JsonRpcResponse.createErrorResponse(request.getMcpRequestId(), getCause(e)));
                    }
                    Exception exception = toException(THROWER.createException(e, request));
                    HandlerFilterFunction<ServerResponse, ServerResponse> errorFunction = CloudUtils.getErrorFunction(target);
                    if (errorFunction != null) {
                        // Apply the error function to handle the exception
                        return errorFunction.filter(req, r -> {
                            throw exception;
                        });
                    } else {
                        // If no error function is available, throw the exception directly
                        throw exception;
                    }
                }
            } else {
                return handler.handle(req);
            }
        };
    }
}
