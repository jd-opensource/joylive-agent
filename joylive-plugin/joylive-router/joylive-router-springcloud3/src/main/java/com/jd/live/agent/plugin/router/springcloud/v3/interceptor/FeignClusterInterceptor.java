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
package com.jd.live.agent.plugin.router.springcloud.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.plugin.router.springcloud.v3.cluster.FeignCluster;
import com.jd.live.agent.plugin.router.springcloud.v3.request.FeignClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.response.FeignClusterResponse;
import feign.Client;
import feign.Request;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FeignClusterInterceptor
 *
 * @since 1.0.0
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
        FeignCluster cluster = clusters.computeIfAbsent((Client) ctx.getTarget(), FeignCluster::new);
        FeignClusterRequest request = new FeignClusterRequest((Request) arguments[0],
                cluster.getLoadBalancerFactory(), cluster.getLoadBalancerProperties(), (Request.Options) arguments[1]);
        HttpOutboundInvocation<FeignClusterRequest> invocation = new HttpOutboundInvocation<>(request, context);
        FeignClusterResponse response = cluster.request(invocation);
        ServiceError error = response.getError();
        if (error != null && !error.isServerError()) {
            mc.setThrowable(error.getThrowable());
        } else {
            mc.setResult(response.getResponse());
        }
        mc.setSkip(true);
    }
}
