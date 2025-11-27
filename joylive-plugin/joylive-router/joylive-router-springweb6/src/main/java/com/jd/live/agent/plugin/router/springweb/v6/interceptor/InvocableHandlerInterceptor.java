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
import com.jd.live.agent.core.parser.JsonPathParser;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InboundInvocation.HttpInboundInvocation;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springweb.v6.request.ServletInboundRequest;
import com.jd.live.agent.plugin.router.springweb.v6.util.CloudUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

import static com.jd.live.agent.plugin.router.springweb.v6.exception.SpringInboundThrower.THROWER;

/**
 * InvocableHandlerInterceptor
 */
public class InvocableHandlerInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final JsonPathParser parser;

    public InvocableHandlerInterceptor(InvocationContext context, JsonPathParser parser) {
        this.context = context;
        this.parser = parser;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        if (!(ctx.getTarget() instanceof ServletInvocableHandlerMethod)) {
            return;
        }
        MethodContext mc = (MethodContext) ctx;
        NativeWebRequest webRequest = ctx.getArgument(0);
        HttpServletRequest servletRequest = (HttpServletRequest) webRequest.getNativeRequest();
        ServletInboundRequest request = new ServletInboundRequest(
                servletRequest,
                ctx.getArguments(),
                CloudUtils.getHandler(ctx.getTarget()),
                context.getGovernanceConfig(),
                parser);
        if (!request.isSystem()) {
            HttpInboundInvocation<ServletInboundRequest> invocation = new HttpInboundInvocation<>(request, context);
            context.inward(invocation, mc::invokeOrigin, (v, e) -> {
                if (e == null) {
                    mc.skipWithResult(v);
                } else {
                    mc.skipWithThrowable(THROWER.createException(e, request));
                }
            });
        }
    }
}
