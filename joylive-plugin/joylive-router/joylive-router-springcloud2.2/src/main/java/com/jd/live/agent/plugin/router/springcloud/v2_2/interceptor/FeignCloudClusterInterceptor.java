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
package com.jd.live.agent.plugin.router.springcloud.v2_2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.plugin.router.springcloud.v2_2.cluster.FeignCloudCluster;
import com.jd.live.agent.plugin.router.springcloud.v2_2.request.FeignCloudClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v2_2.response.FeignClusterResponse;
import feign.Client;
import feign.Request;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FeignClusterInterceptor
 *
 * @since 1.0.0
 */
public class FeignCloudClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final Map<Client, FeignCloudCluster> clusters = new ConcurrentHashMap<>();

    public FeignCloudClusterInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Request request = ctx.getArgument(0);
        if (context.isFlowControlEnabled()) {
            FeignCloudCluster cluster = clusters.computeIfAbsent((Client) ctx.getTarget(), FeignCloudCluster::new);
            FeignCloudClusterRequest clusterRequest = new FeignCloudClusterRequest(request, ctx.getArgument(1), cluster.getContext());
            HttpOutboundInvocation<FeignCloudClusterRequest> invocation = new HttpOutboundInvocation<>(clusterRequest, context);
            FeignClusterResponse response = cluster.request(invocation);
            ServiceError error = response.getError();
            if (error != null && !error.isServerError()) {
                mc.skipWithThrowable(error.getThrowable());
            } else {
                mc.skipWithResult(response.getResponse());
            }
        } else {
            // only for live & lane
            String serviceName = URI.parseHost(request.url());
            RequestContext.setAttribute(Carrier.ATTRIBUTE_SERVICE_ID, serviceName);
            RequestContext.setAttribute(Carrier.ATTRIBUTE_REQUEST, request);
        }
    }
}
