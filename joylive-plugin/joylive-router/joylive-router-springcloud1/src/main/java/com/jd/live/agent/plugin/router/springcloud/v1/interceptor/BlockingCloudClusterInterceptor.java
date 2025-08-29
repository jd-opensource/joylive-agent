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
package com.jd.live.agent.plugin.router.springcloud.v1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.plugin.router.springcloud.v1.cluster.BlockingCloudCluster;
import com.jd.live.agent.plugin.router.springcloud.v1.request.BlockingCloudClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v1.response.BlockingClusterResponse;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.http.HttpRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BlockingCloudClusterInterceptor
 *
 * @since 1.9.0
 */
public abstract class BlockingCloudClusterInterceptor extends AbstractCloudClusterInterceptor<HttpRequest> {

    private final Map<Object, BlockingCloudCluster> clusters = new ConcurrentHashMap<>();

    protected BlockingCloudClusterInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    protected void request(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        HttpRequest request = ctx.getArgument(0);
        BlockingCloudCluster cluster = clusters.computeIfAbsent(ctx.getTarget(), this::createCluster);
        BlockingCloudClusterRequest clusterRequest = new BlockingCloudClusterRequest(request, ctx.getArgument(1), ctx.getArgument(2), cluster.getContext());
        HttpOutboundInvocation<BlockingCloudClusterRequest> invocation = new HttpOutboundInvocation<>(clusterRequest, context);
        BlockingClusterResponse response = cluster.request(invocation);
        ServiceError error = response.getError();
        if (error != null && !error.isServerError()) {
            mc.skipWithThrowable(error.getThrowable());
        } else {
            mc.skipWithResult(response.getResponse());
        }
    }

    protected abstract BlockingCloudCluster createCluster(Object interceptor);

    @Override
    protected String getServiceName(HttpRequest request) {
        return request.getURI().getHost();
    }

    /**
     * LoadBalancerClusterInterceptor
     *
     * @since 1.9.0
     */
    public static class LoadBalancerClusterInterceptor extends BlockingCloudClusterInterceptor {

        public LoadBalancerClusterInterceptor(InvocationContext context) {
            super(context);
        }

        @Override
        protected BlockingCloudCluster createCluster(Object interceptor) {
            return new BlockingCloudCluster(context.getRegistry(), (LoadBalancerInterceptor) interceptor);
        }
    }

    /**
     * RetryLoadBalancerClusterInterceptor
     *
     * @since 1.9.0
     */
    public static class RetryLoadBalancerClusterInterceptor extends BlockingCloudClusterInterceptor {

        public RetryLoadBalancerClusterInterceptor(InvocationContext context) {
            super(context);
        }

        @Override
        protected BlockingCloudCluster createCluster(Object interceptor) {
            return new BlockingCloudCluster(context.getRegistry(), (RetryLoadBalancerInterceptor) interceptor);
        }
    }
}
