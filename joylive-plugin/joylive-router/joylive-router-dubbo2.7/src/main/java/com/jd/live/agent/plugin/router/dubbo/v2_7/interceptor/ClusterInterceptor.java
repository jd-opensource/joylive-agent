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
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.plugin.router.dubbo.v2_7.instance.DubboEndpoint;
import com.jd.live.agent.plugin.router.dubbo.v2_7.request.DubboRequest.DubboOutboundRequest;
import com.jd.live.agent.plugin.router.dubbo.v2_7.request.invoke.DubboInvocation.DubboOutboundInvocation;
import com.jd.live.agent.plugin.router.dubbo.v2_7.response.DubboResponse.DubboOutboundResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;
import org.apache.dubbo.rpc.cluster.support.Dubbo27Cluster;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * ClusterInterceptor
 */
public class ClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final ObjectParser parser;

    private final Map<AbstractClusterInvoker<?>, Dubbo27Cluster> clusters = new ConcurrentHashMap<>();

    public ClusterInterceptor(InvocationContext context, ObjectParser parser) {
        this.context = context;
        this.parser = parser;
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
        Dubbo27Cluster cluster = clusters.computeIfAbsent((AbstractClusterInvoker<?>) ctx.getTarget(),
                invoker -> new Dubbo27Cluster(invoker, parser));
        List<Invoker<?>> invokers = (List<Invoker<?>>) arguments[1];
        List<DubboEndpoint<?>> instances = invokers.stream().map(DubboEndpoint::of).collect(Collectors.toList());
        Invocation invocation = (Invocation) arguments[0];
        DubboOutboundRequest request = new DubboOutboundRequest(invocation);
        if (!request.isSystem() && !request.isDisabled()) {
            DubboOutboundResponse response = cluster.request(new DubboOutboundInvocation(request, context), instances);
            ServiceError error = response.getError();
            if (error != null && !error.isServerError()) {
                mc.setThrowable(error.getThrowable());
            } else {
                mc.setResult(response.getResponse());
            }
            mc.setSkip(true);
        }
    }
}
