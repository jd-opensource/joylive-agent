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

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.RouterChain;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.governance.interceptor.AbstractInterceptor.AbstractRouteInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilterChain;
import com.jd.live.agent.plugin.router.sofarpc.instance.SofaRpcEndpoint;
import com.jd.live.agent.plugin.router.sofarpc.request.SofaRpcRequest.SofaRpcOutboundRequest;
import com.jd.live.agent.plugin.router.sofarpc.request.invoke.SofaRpcInvocation.SofaRpcOutboundInvocation;

import java.util.List;
import java.util.stream.Collectors;

import static com.alipay.sofa.rpc.core.exception.RpcErrorType.CLIENT_ROUTER;

/**
 * RouterChainInterceptor
 */
public class RouterChainInterceptor extends AbstractRouteInterceptor<SofaRpcOutboundRequest, SofaRpcOutboundInvocation> {

    public RouterChainInterceptor(InvocationContext context, List<RouteFilter> filters) {
        super(context, filters);
    }

    /**
     * Enhanced logic after method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     * @see RouterChain#route(SofaRequest, List)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object result = mc.getResult();
        Object[] arguments = mc.getArguments();
        List<ProviderInfo> invokers = (List<ProviderInfo>) result;
        List<SofaRpcEndpoint> instances = invokers.stream().map(SofaRpcEndpoint::new).collect(Collectors.toList());
        SofaRequest request = (SofaRequest) arguments[0];
        SofaRpcOutboundRequest outboundRequest = new SofaRpcOutboundRequest(request);
        try {
            SofaRpcOutboundInvocation outboundInvocation = routing(outboundRequest, instances);
            List<SofaRpcEndpoint> endpoints = (List<SofaRpcEndpoint>) outboundInvocation.getEndpoints();
            mc.setResult(endpoints.stream().map(SofaRpcEndpoint::getProvider).collect(Collectors.toList()));
        } catch (RejectException e) {
            mc.setThrowable(new SofaRpcException(CLIENT_ROUTER, e.getMessage()));
        }
    }

    @Override
    protected void routing(SofaRpcOutboundInvocation invocation) {
        new RouteFilterChain.Chain(routeFilters).filter(invocation);
    }

    @Override
    protected SofaRpcOutboundInvocation createOutlet(SofaRpcOutboundRequest request) {
        return new SofaRpcOutboundInvocation(request, context);
    }
}
