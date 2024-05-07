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
import com.jd.live.agent.plugin.router.sofarpc.request.invoke.SofaRpcInvocation;
import com.jd.live.agent.plugin.router.sofarpc.response.SofaRpcResponse;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * ConsumerInvokerInterceptor
 */
public class ConsumerInvokerInterceptor extends
        AbstractOutboundInterceptor<SofaRpcOutboundRequest, SofaRpcInvocation.SofaRpcOutboundInvocation> {

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
        SofaRpcInvocation.SofaRpcOutboundInvocation outboundInvocation = null;
        try {
            outboundInvocation = process(new SofaRpcOutboundRequest(request));
        } catch (RejectException e) {
            SofaResponse response = new SofaResponse();
            response.setErrorMsg(e.getMessage());
            mc.setResult(response);
            mc.setSkip(true);
        }
        final Supplier<Response> retrySupplier = createRetrySupplier(mc.getTarget(), mc.getMethod(), mc.getArguments(), mc.getResult());
        Response result = null;
        Throwable ex = null;
        try {
            result = retrySupplier.get();
        } catch (Throwable throwable) {
            ex = throwable;
        }
        Response tryResult = tryRetry(outboundInvocation, result, retrySupplier);
        if (tryResult != null) {
            result = tryResult;
        }
        mc.setResult(result == null ? null : result.getResponse());
        mc.setSkip(true);
    }

    @Override
    protected void process(SofaRpcInvocation.SofaRpcOutboundInvocation invocation) {
        new OutboundFilterChain.Chain(outboundFilters).filter(invocation);
    }

    @Override
    protected SofaRpcInvocation.SofaRpcOutboundInvocation createOutlet(SofaRpcOutboundRequest request) {
        return new SofaRpcInvocation.SofaRpcOutboundInvocation(request, context);
    }

    @Override
    protected Supplier<Response> createRetrySupplier(Object target, Method method, Object[] allArguments, Object result) {
        return () -> {
            Response response = null;
            method.setAccessible(true);
            try {
                Object r = method.invoke(target, allArguments);
                response = new SofaRpcResponse.SofaRpcOutboundResponse((SofaResponse) r, null);
            } catch (IllegalAccessException ignored) {
                // ignored
            } catch (Throwable throwable) {
                response = new SofaRpcResponse.SofaRpcOutboundResponse((SofaResponse) result, throwable);
            }
            return response;
        };
    }
}
