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
package com.jd.live.agent.plugin.router.springcloud.v2_1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.plugin.router.springcloud.v2_1.cluster.HttpClientCloudCluster;
import com.jd.live.agent.plugin.router.springcloud.v2_1.request.HttpClientClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v2_1.response.HttpClientClusterResponse;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.cloud.netflix.ribbon.support.AbstractLoadBalancingClient;

import java.io.IOException;

/**
 * RibbonCloudClientClusterInterceptor
 */
public class RibbonCloudClientClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public RibbonCloudClientClusterInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        // in constructor method
        RibbonLoadBalancingHttpClient target = (RibbonLoadBalancingHttpClient) ctx.getTarget();
        // recreate delegate client
        LiveHttpClient client = new LiveHttpClient(target, target.getDelegate(), context);
        client.update();
    }

    /**
     * A delegating HTTP client that routes requests through a cloud cluster with Ribbon load balancing.
     * Handles cluster-aware execution and error response conversion.
     */
    private static class LiveHttpClient extends CloseableHttpClient {

        protected static final FieldAccessor delegateAccessor = FieldAccessorFactory.getAccessor(AbstractLoadBalancingClient.class, "delegate");

        private final RibbonLoadBalancingHttpClient client;

        private final CloseableHttpClient delegate;

        private final InvocationContext context;

        private final HttpClientCloudCluster cluster;

        LiveHttpClient(RibbonLoadBalancingHttpClient client, CloseableHttpClient delegate, InvocationContext context) {
            this.client = client;
            this.delegate = delegate;
            this.context = context;
            this.cluster = new HttpClientCloudCluster(context.getRegistry(), client);
        }

        @Override
        protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
            HttpClientClusterRequest clusterRequest = new HttpClientClusterRequest(request, context, delegate, cluster.getContext());
            HttpOutboundInvocation<HttpClientClusterRequest> invocation = new HttpOutboundInvocation<>(clusterRequest, this.context);
            HttpClientClusterResponse response = cluster.request(invocation);
            ServiceError error = response.getError();
            if (error != null && !error.isServerError()) {
                if (error.getThrowable() instanceof IOException) {
                    throw (IOException) error.getThrowable();
                }
                throw new ClientProtocolException(error.getError(), error.getThrowable());
            } else {
                return response.getResponse();
            }
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Deprecated
        @Override
        public HttpParams getParams() {
            return delegate.getParams();
        }

        @Deprecated
        @Override
        public ClientConnectionManager getConnectionManager() {
            return delegate.getConnectionManager();
        }

        public void update() {
            delegateAccessor.set(client, this);
        }

    }
}
