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
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.invoke.InboundInvocation.HttpInboundInvocation;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springweb.v5.request.ExceptionView;
import com.jd.live.agent.plugin.router.springweb.v5.request.ServletInboundRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

/**
 * HandlerAdapterInterceptor
 */
public class HandlerAdapterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public HandlerAdapterInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        ServiceConfig config =  context.getGovernanceConfig().getServiceConfig();
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = ctx.getArguments();
        ServletInboundRequest request = new ServletInboundRequest((HttpServletRequest) arguments[0], arguments[2], config::isSystem);
        if (!request.isSystem()) {
            HttpInboundInvocation<ServletInboundRequest> invocation = new HttpInboundInvocation<>(request, context);
            ModelAndView view = context.inward(invocation, mc::invokeOrigin, request::convert);
            if (view instanceof ExceptionView) {
                mc.skipWithThrowable(((ExceptionView) view).getThrowable());
            } else {
                mc.skipWithResult(view);
            }
        }
    }
}
