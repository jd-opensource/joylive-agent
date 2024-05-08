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
import com.jd.live.agent.governance.interceptor.AbstractInterceptor.AbstractOutboundInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilterChain;
import com.jd.live.agent.governance.response.Response;
import com.jd.live.agent.plugin.router.dubbo.v3.request.DubboRequest.DubboOutboundRequest;
import com.jd.live.agent.plugin.router.dubbo.v3.request.invoke.DubboInvocation.DubboOutboundInvocation;
import com.jd.live.agent.plugin.router.dubbo.v3.response.DubboResponse.DubboOutboundResponse;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.filter.support.ConsumerClassLoaderFilter;

import java.util.List;

/**
 * ConsumerClassLoaderFilterInterceptor
 */
public class ConsumerClassLoaderFilterInterceptor extends
        AbstractOutboundInterceptor<DubboOutboundRequest, DubboOutboundInvocation> {

    public ConsumerClassLoaderFilterInterceptor(InvocationContext context, List<OutboundFilter> filters) {
        super(context, filters);
    }

    /**
     * Enhanced logic after method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     * @see ConsumerClassLoaderFilter#invoke(Invoker, Invocation)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = mc.getArguments();
        Invocation invocation = (Invocation) arguments[1];
        Object result;
        try {
            DubboOutboundInvocation outboundInvocation = process(new DubboOutboundRequest(invocation));
            result = invokeWithRetry(outboundInvocation, mc);
        } catch (RejectException e) {
            result = new AppResponse(new RpcException(RpcException.FORBIDDEN_EXCEPTION, e.getMessage()));
        }
        mc.setResult(result);
        mc.setSkip(true);
    }

    @Override
    protected void process(DubboOutboundInvocation invocation) {
        new OutboundFilterChain.Chain(outboundFilters).filter(invocation);
    }

    @Override
    protected DubboOutboundInvocation createOutlet(DubboOutboundRequest request) {
        return new DubboOutboundInvocation(request, context);
    }

    @Override
    protected Response createResponse(Object result, Throwable throwable) {
        return new DubboOutboundResponse((Result) result, throwable);
    }
}
