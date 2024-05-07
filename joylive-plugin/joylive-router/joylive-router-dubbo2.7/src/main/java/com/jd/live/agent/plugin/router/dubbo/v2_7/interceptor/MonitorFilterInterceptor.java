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
package com.jd.live.agent.plugin.router.dubbo.v2_7.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.governance.interceptor.AbstractInterceptor.AbstractOutboundInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilterChain;
import com.jd.live.agent.governance.invoke.retry.RetrierFactory;
import com.jd.live.agent.governance.response.Response;
import com.jd.live.agent.plugin.router.dubbo.v2_7.request.DubboRequest.DubboOutboundRequest;
import com.jd.live.agent.plugin.router.dubbo.v2_7.request.invoke.DubboInvocation.DubboOutboundInvocation;
import com.jd.live.agent.plugin.router.dubbo.v2_7.response.DubboResponse;
import org.apache.dubbo.monitor.support.MonitorFilter;
import org.apache.dubbo.rpc.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * MonitorFilterInterceptor
 */
public class MonitorFilterInterceptor extends
        AbstractOutboundInterceptor<DubboOutboundRequest, DubboOutboundInvocation> {

    public MonitorFilterInterceptor(InvocationContext context, List<OutboundFilter> filters, Map<String, RetrierFactory> retrierFactories) {
        super(context, filters, retrierFactories);
    }

    /**
     * Enhanced logic after method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     * @see MonitorFilter#invoke(Invoker, Invocation)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = mc.getArguments();
        Invocation invocation = (Invocation) arguments[1];
        DubboOutboundInvocation outboundInvocation = null;
        try {
            outboundInvocation = process(new DubboOutboundRequest(invocation));
        } catch (RejectException e) {
            Result result = new AppResponse(new RpcException(RpcException.FORBIDDEN_EXCEPTION, e.getMessage()));
            mc.setResult(result);
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
    protected void process(DubboOutboundInvocation invocation) {
        new OutboundFilterChain.Chain(outboundFilters).filter(invocation);
    }

    @Override
    protected DubboOutboundInvocation createOutlet(DubboOutboundRequest request) {
        return new DubboOutboundInvocation(request, context);
    }

    @Override
    protected Supplier<Response> createRetrySupplier(Object target, Method method, Object[] allArguments, Object result) {
        return () -> {
            Response response = null;
            method.setAccessible(true);
            try {
                Object r = method.invoke(target, allArguments);
                response = new DubboResponse.DubboOutboundResponse((Result) r, null);
            } catch (IllegalAccessException ignored) {
                // ignored
            } catch (Throwable throwable) {
                response = new DubboResponse.DubboOutboundResponse((Result) result, throwable);
            }
            return response;
        };
    }
}
