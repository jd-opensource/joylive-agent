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
package com.jd.live.agent.plugin.router.sofarpc.interceptor;

import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.filter.ProviderInvoker;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.governance.interceptor.AbstractInterceptor.AbstractInboundInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.InboundFilterChain;
import com.jd.live.agent.plugin.router.sofarpc.request.SofaRpcRequest.SofaRpcInboundRequest;
import com.jd.live.agent.plugin.router.sofarpc.request.invoke.DubboInvocation;

import java.util.List;

/**
 * ProviderInvokerInterceptor
 */
public class ProviderInvokerInterceptor extends
        AbstractInboundInterceptor<SofaRpcInboundRequest, DubboInvocation.DubboInboundInvocation> {

    public ProviderInvokerInterceptor(InvocationContext context, List<InboundFilter> filters) {
        super(context, filters);
    }

    /**
     * Enhanced logic after method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     * @see ProviderInvoker#invoke(SofaRequest)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        SofaRequest request = (SofaRequest) mc.getArguments()[0];
        try {
            process(new SofaRpcInboundRequest(request));
        } catch (RejectException e) {
            SofaResponse response = new SofaResponse();
            response.setErrorMsg(e.getMessage());
            mc.setResult(response);
            mc.setSkip(true);
        }
    }

    @Override
    protected void process(DubboInvocation.DubboInboundInvocation invocation) {
        new InboundFilterChain.Chain(inboundFilters).filter(invocation);
    }

    @Override
    protected DubboInvocation.DubboInboundInvocation createInlet(SofaRpcInboundRequest request) {
        return new DubboInvocation.DubboInboundInvocation(request, context);
    }

}
