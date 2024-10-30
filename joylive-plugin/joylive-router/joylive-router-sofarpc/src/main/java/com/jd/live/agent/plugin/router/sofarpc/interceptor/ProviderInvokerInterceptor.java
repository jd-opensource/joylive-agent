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
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.sofarpc.request.SofaRpcRequest.SofaRpcInboundRequest;
import com.jd.live.agent.plugin.router.sofarpc.request.invoke.SofaRpcInvocation.SofaRpcInboundInvocation;

/**
 * ProviderInvokerInterceptor
 */
public class ProviderInvokerInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public ProviderInvokerInterceptor(InvocationContext context) {
        this.context = context;
    }

    /**
     * Enhanced logic before method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     * @see ProviderInvoker#invoke(SofaRequest)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        SofaRpcInboundRequest request = new SofaRpcInboundRequest((SofaRequest) mc.getArguments()[0]);
        SofaRpcInboundInvocation invocation = new SofaRpcInboundInvocation(request, context);
        SofaResponse result = context.inward(invocation, mc::invokeOrigin, request::convert);
        mc.skipWithResult(result);
    }
}
