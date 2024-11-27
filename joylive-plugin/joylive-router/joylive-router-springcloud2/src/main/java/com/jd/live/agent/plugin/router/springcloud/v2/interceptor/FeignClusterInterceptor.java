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
package com.jd.live.agent.plugin.router.springcloud.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.plugin.router.springcloud.v2.cluster.FeignCluster;
import com.jd.live.agent.plugin.router.springcloud.v2.request.FeignClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v2.response.FeignClusterResponse;
import feign.Client;
import feign.Request;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FeignClusterInterceptor
 *
 * @since 1.5.0
 */
public class FeignClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final Map<Client, FeignCluster> clusters = new ConcurrentHashMap<>();

    public FeignClusterInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = ctx.getArguments();
        Request request = (Request) arguments[0];
        if (context.isFlowControlEnabled()) {
            FeignCluster cluster = clusters.computeIfAbsent((Client) ctx.getTarget(), FeignCluster::new);
            FeignClusterRequest clusterRequest = new FeignClusterRequest(request,
                    cluster.getLoadBalancerFactory(), (Request.Options) arguments[1]);
            HttpOutboundInvocation<FeignClusterRequest> invocation = new HttpOutboundInvocation<>(clusterRequest, context);
            FeignClusterResponse response = cluster.request(invocation);
            ServiceError error = response.getError();
            if (error != null && !error.isServerError()) {
                mc.skipWithThrowable(error.getThrowable());
            } else {
                mc.skipWithResult(response.getResponse());
            }
        } else {
            // only for live & lane
            URI uri = URI.parse(request.url());
            String serviceName = uri.getHost();
            RequestContext.setAttribute(Carrier.ATTRIBUTE_SERVICE_ID, serviceName);
            RequestContext.setAttribute(Carrier.ATTRIBUTE_REQUEST, request);
        }
    }
}
