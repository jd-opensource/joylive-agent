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
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.dubbo.v3.instance.DubboEndpoint;
import com.jd.live.agent.plugin.router.dubbo.v3.request.DubboRequest.DubboOutboundRequest;
import com.jd.live.agent.plugin.router.dubbo.v3.request.invoke.DubboInvocation.DubboOutboundInvocation;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;
import org.apache.dubbo.rpc.cluster.support.v3.DubboCluster;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * LoadBalanceInterceptor
 */
public class LoadBalanceInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final Map<AbstractClusterInvoker<?>, DubboCluster> clusters = new ConcurrentHashMap<>();

    public LoadBalanceInterceptor(InvocationContext context) {
        this.context = context;
    }

    /**
     * Enhanced logic after method execution<br>
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
        DubboOutboundInvocation invocation = new DubboOutboundInvocation(request, context);
        DubboCluster cluster = clusters.computeIfAbsent((AbstractClusterInvoker<?>) ctx.getTarget(), DubboCluster::new);
        try {
            List<DubboEndpoint<?>> instances = invokers.stream().map(DubboEndpoint::of).collect(Collectors.toList());
            if (invoked != null) {
                invoked.forEach(p -> request.addAttempt(new DubboEndpoint<>(p).getId()));
            }
            List<? extends Endpoint> endpoints = context.route(invocation, instances);
            if (endpoints != null && !endpoints.isEmpty()) {
                mc.setResult(((DubboEndpoint<?>) endpoints.get(0)).getInvoker());
            } else {
                mc.setThrowable(cluster.createNoProviderException(request));
            }
        } catch (RejectException e) {
            mc.setThrowable(cluster.createRejectException(e, request));
        }
        mc.setSkip(true);
    }

}
