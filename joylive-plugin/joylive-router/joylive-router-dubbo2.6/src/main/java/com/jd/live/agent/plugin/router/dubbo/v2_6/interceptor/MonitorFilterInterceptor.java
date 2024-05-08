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
package com.jd.live.agent.plugin.router.dubbo.v2_6.interceptor;

import com.alibaba.dubbo.rpc.*;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.governance.interceptor.AbstractInterceptor.AbstractOutboundInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilterChain;
import com.jd.live.agent.governance.response.Response;
import com.jd.live.agent.plugin.router.dubbo.v2_6.request.DubboRequest.DubboOutboundRequest;
import com.jd.live.agent.plugin.router.dubbo.v2_6.request.invoke.DubboInvocation.DubboOutboundInvocation;
import com.jd.live.agent.plugin.router.dubbo.v2_6.response.DubboResponse.DubboOutboundResponse;

import java.util.List;

/**
 * MonitorFilterInterceptor
 */
public class MonitorFilterInterceptor extends
        AbstractOutboundInterceptor<DubboOutboundRequest, DubboOutboundInvocation> {

    public MonitorFilterInterceptor(InvocationContext context, List<OutboundFilter> filters) {
        super(context, filters);
    }

    /**
     * Enhanced logic after method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     * @see com.alibaba.dubbo.monitor.support.MonitorFilter#invoke(Invoker, Invocation)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Invocation invocation = (Invocation) mc.getArguments()[1];
        Object result;
        try {
            DubboOutboundInvocation outboundInvocation = process(new DubboOutboundRequest(invocation));
            result = invokeWithRetry(outboundInvocation, mc);
        } catch (RejectException e) {
            result = new RpcResult(new RpcException(RpcException.FORBIDDEN_EXCEPTION, e.getMessage()));
        }
        mc.setResult(result);
        mc.setSkip(true);
    }

    @Override
    protected void process(DubboOutboundInvocation invocation) {
        new OutboundFilterChain.Chain(outboundFilters).filter(invocation);
    }

    @Override
    protected Response createResponse(Object result, Throwable throwable) {
        return new DubboOutboundResponse((Result) result, throwable);
    }

    @Override
    protected DubboOutboundInvocation createOutlet(DubboOutboundRequest request) {
        return new DubboOutboundInvocation(request, context);
    }
}
