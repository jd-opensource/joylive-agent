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

import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.cluster.support.AbstractClusterInvoker;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.dubbo.v2_6.exception.Dubbo26OutboundThrower;
import com.jd.live.agent.plugin.router.dubbo.v2_6.instance.DubboEndpoint;
import com.jd.live.agent.plugin.router.dubbo.v2_6.request.DubboRequest.DubboOutboundRequest;
import com.jd.live.agent.plugin.router.dubbo.v2_6.request.invoke.DubboInvocation.DubboOutboundInvocation;

import java.util.List;

/**
 * LoadBalanceInterceptor
 */
public class LoadBalanceInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(LoadBalanceInterceptor.class);

    private final InvocationContext context;

    public LoadBalanceInterceptor(InvocationContext context) {
        this.context = context;
    }

    /**
     * Enhanced logic before method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = ctx.getArguments();
        List<Invoker<?>> invokers = (List<Invoker<?>>) arguments[2];
        List<Invoker<?>> invoked = (List<Invoker<?>>) arguments[3];
        DubboOutboundRequest request = new DubboOutboundRequest((Invocation) arguments[1]);
        if (!request.isSystem() && !request.isDisabled()) {
            try {
                DubboOutboundInvocation invocation = new DubboOutboundInvocation(request, context);
                if (invoked != null) {
                    invoked.forEach(p -> request.addAttempt(new DubboEndpoint<>(p).getId()));
                }
                DubboEndpoint<?> endpoint = context.route(invocation, invokers, DubboEndpoint::of);
                mc.skipWithResult(endpoint.getInvoker());
            } catch (Throwable e) {
                logger.error("Exception occurred when routing, caused by " + e.getMessage(), e);
                Dubbo26OutboundThrower thrower = new Dubbo26OutboundThrower((AbstractClusterInvoker<?>) ctx.getTarget());
                mc.skipWithThrowable(thrower.createException(e, request));
            }
        }
    }

}
