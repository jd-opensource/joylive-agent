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
import com.alipay.sofa.rpc.filter.ConsumerInvoker;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.governance.interceptor.AbstractInterceptor.AbstractOutboundInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilterChain;
import com.jd.live.agent.governance.invoke.retry.RetrierFactory;
import com.jd.live.agent.governance.response.Response;
import com.jd.live.agent.plugin.router.sofarpc.request.SofaRpcRequest.SofaRpcOutboundRequest;
import com.jd.live.agent.plugin.router.sofarpc.request.invoke.SofaRpcInvocation.SofaRpcOutboundInvocation;
import com.jd.live.agent.plugin.router.sofarpc.response.SofaRpcResponse.SofaRpcOutboundResponse;

import java.util.List;
import java.util.Map;

/**
 * ConsumerInvokerInterceptor
 */
public class ConsumerInvokerInterceptor extends
        AbstractOutboundInterceptor<SofaRpcOutboundRequest, SofaRpcOutboundInvocation> {

    public ConsumerInvokerInterceptor(InvocationContext context, List<OutboundFilter> filters, Map<String, RetrierFactory> retrierFactories) {
        super(context, filters, retrierFactories);
    }

    /**
     * Enhanced logic after method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     * @see ConsumerInvoker#invoke(SofaRequest)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        SofaRequest request = (SofaRequest) mc.getArguments()[0];
        Object result;
        try {
            SofaRpcOutboundInvocation outboundInvocation = process(new SofaRpcOutboundRequest(request));
            result = invokeWithRetry(outboundInvocation, mc);
        } catch (RejectException e) {
            SofaResponse response = new SofaResponse();
            response.setErrorMsg(e.getMessage());
            result = response;
        }
        mc.setResult(result);
        mc.setSkip(true);
    }

    @Override
    protected void process(SofaRpcOutboundInvocation invocation) {
        new OutboundFilterChain.Chain(outboundFilters).filter(invocation);
    }

    @Override
    protected SofaRpcOutboundInvocation createOutlet(SofaRpcOutboundRequest request) {
        return new SofaRpcOutboundInvocation(request, context);
    }

    @Override
    protected Response createResponse(Object result, Throwable throwable) {
        return new SofaRpcOutboundResponse((SofaResponse) result, throwable);
    }
}
