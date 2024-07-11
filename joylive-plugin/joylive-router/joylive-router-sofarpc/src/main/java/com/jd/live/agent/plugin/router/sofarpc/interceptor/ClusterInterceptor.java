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

import com.alipay.sofa.rpc.client.AbstractCluster;
import com.alipay.sofa.rpc.client.SofaRpcCluster;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.sofarpc.request.SofaRpcRequest.SofaRpcOutboundRequest;
import com.jd.live.agent.plugin.router.sofarpc.request.invoke.SofaRpcInvocation.SofaRpcOutboundInvocation;
import com.jd.live.agent.plugin.router.sofarpc.response.SofaRpcResponse.SofaRpcOutboundResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClusterInterceptor
 */
public class ClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final ObjectParser parser;

    private final Map<AbstractCluster, SofaRpcCluster> clusters = new ConcurrentHashMap<>();

    public ClusterInterceptor(InvocationContext context, ObjectParser parser) {
        this.context = context;
        this.parser = parser;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = ctx.getArguments();
        SofaRpcCluster cluster = clusters.computeIfAbsent((AbstractCluster) ctx.getTarget(),
                c -> new SofaRpcCluster(c, parser));
        SofaRpcOutboundRequest request = new SofaRpcOutboundRequest((SofaRequest) arguments[0], cluster);
        if (!request.isSystem() && !request.isDisabled()) {
            SofaRpcOutboundInvocation invocation = new SofaRpcOutboundInvocation(request, new SofaRpcInvocationContext(context));
            SofaRpcOutboundResponse response = cluster.request(context, invocation, null);
            if (response.getThrowable() != null) {
                mc.setThrowable(response.getThrowable());
            } else {
                mc.setResult(response.getResponse());
            }
            mc.setSkip(true);
        }
    }
}
