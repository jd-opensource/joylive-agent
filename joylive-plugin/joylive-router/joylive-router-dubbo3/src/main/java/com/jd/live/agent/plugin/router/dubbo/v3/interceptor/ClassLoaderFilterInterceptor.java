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
package com.jd.live.agent.plugin.router.dubbo.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.governance.interceptor.AbstractInterceptor.AbstractInboundInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.InboundFilterChain;
import com.jd.live.agent.plugin.router.dubbo.v3.request.DubboRequest.DubboInboundRequest;
import com.jd.live.agent.plugin.router.dubbo.v3.request.invoke.DubboInvocation.DubboInboundInvocation;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.filter.ClassLoaderFilter;

import java.util.List;

/**
 * ClassLoaderFilterInterceptor
 */
public class ClassLoaderFilterInterceptor extends
        AbstractInboundInterceptor<DubboInboundRequest, DubboInboundInvocation> {

    public ClassLoaderFilterInterceptor(InvocationContext context, List<InboundFilter> filters) {
        super(context, filters);
    }

    /**
     * Enhanced logic after method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     * @see ClassLoaderFilter#invoke(Invoker, Invocation)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = mc.getArguments();
        Invocation invocation = (Invocation) arguments[1];
        try {
            process(new DubboInboundRequest(invocation));
        } catch (RejectException e) {
            Result result = new AppResponse(new RpcException(RpcException.FORBIDDEN_EXCEPTION, e.getMessage()));
            mc.setResult(result);
            mc.setSkip(true);
        }
    }

    @Override
    protected void process(DubboInboundInvocation invocation) {
        new InboundFilterChain.Chain(inboundFilters).filter(invocation);
    }

    @Override
    protected DubboInboundInvocation createInlet(DubboInboundRequest request) {
        return new DubboInboundInvocation(request, context);
    }

}
