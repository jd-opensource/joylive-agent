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
import com.alipay.sofa.rpc.client.LiveCluster;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.core.exception.SofaRouteException;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.log.LogCodes;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.interceptor.AbstractInterceptor.AbstractRouteInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.plugin.router.sofarpc.instance.SofaRpcEndpoint;
import com.jd.live.agent.plugin.router.sofarpc.request.SofaRpcRequest.SofaRpcOutboundRequest;
import com.jd.live.agent.plugin.router.sofarpc.request.invoke.SofaRpcInvocation.SofaRpcOutboundInvocation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.alipay.sofa.rpc.core.exception.RpcErrorType.CLIENT_ROUTER;

/**
 * ClusterInterceptor
 */
public class LoadBalanceInterceptor extends AbstractRouteInterceptor<SofaRpcOutboundRequest, SofaRpcOutboundInvocation> {

    private final Map<AbstractCluster, LiveCluster> clusters = new ConcurrentHashMap<>();

    public LoadBalanceInterceptor(InvocationContext context, List<RouteFilter> filters) {
        super(context, filters);
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
        SofaRequest request = (SofaRequest) arguments[0];
        List<ProviderInfo> invoked = (List<ProviderInfo>) arguments[1];
        LiveCluster cluster = clusters.computeIfAbsent((AbstractCluster) ctx.getTarget(), LiveCluster::new);
        SofaRpcOutboundRequest outboundRequest = new SofaRpcOutboundRequest(request, cluster);
        SofaRpcOutboundInvocation invocation = createOutlet(outboundRequest);
        try {
            List<ProviderInfo> invokers = cluster.route(request);
            List<SofaRpcEndpoint> instances = invokers.stream().map(e -> new SofaRpcEndpoint(e, cluster::isConnected)).collect(Collectors.toList());
            invocation.setInstances(instances);
            invoked.forEach(p -> outboundRequest.addAttempt(p.getHost() + ":" + p.getPort()));
            List<? extends Endpoint> endpoints = routing(invocation);
            if (endpoints != null && !endpoints.isEmpty()) {
                mc.setResult(((SofaRpcEndpoint) endpoints.get(0)).getProvider());
            } else {
                mc.setThrowable(new SofaRouteException(
                        LogCodes.getLog(LogCodes.ERROR_NO_AVAILABLE_PROVIDER,
                                request.getTargetServiceUniqueName(), "[]")));
            }
        } catch (RejectException e) {
            mc.setThrowable(new SofaRpcException(CLIENT_ROUTER, e.getMessage()));
        }
        mc.setSkip(true);
    }

    @Override
    protected SofaRpcOutboundInvocation createOutlet(SofaRpcOutboundRequest request) {
        return new SofaRpcOutboundInvocation(request, new SofaRpcInvocationContext(context));
    }

}
