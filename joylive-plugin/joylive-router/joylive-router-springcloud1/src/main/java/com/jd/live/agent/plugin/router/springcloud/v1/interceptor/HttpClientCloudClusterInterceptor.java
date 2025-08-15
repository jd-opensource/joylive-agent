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
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.plugin.router.springcloud.v1.cluster.HttpClientCloudCluster;
import com.jd.live.agent.plugin.router.springcloud.v1.request.HttpClientClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v1.response.HttpClientClusterResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.cloud.netflix.ribbon.support.AbstractLoadBalancingClient;

import java.io.IOException;
import java.net.URI;

/**
 * HttpClientClusterInterceptor
 */
public class HttpClientCloudClusterInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientCloudClusterInterceptor.class);

    private final InvocationContext context;

    public HttpClientCloudClusterInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        RibbonLoadBalancingHttpClient target = (RibbonLoadBalancingHttpClient) ctx.getTarget();
        LiveHttpClient client = new LiveHttpClient(target, target.getDelegate(), context);
        client.update();
    }

    /**
     * A delegating HTTP client that routes requests through a cloud cluster with Ribbon load balancing.
     * Handles cluster-aware execution and error response conversion.
     */
    private static class LiveHttpClient implements HttpClient {

        protected static final FieldAccessor delegateAccessor = FieldAccessorFactory.getAccessor(AbstractLoadBalancingClient.class, "delegate");

        private final RibbonLoadBalancingHttpClient client;

        private final HttpClient delegate;

        private final InvocationContext context;

        private final HttpClientCloudCluster cluster;

        LiveHttpClient(RibbonLoadBalancingHttpClient client, HttpClient delegate, InvocationContext context) {
            this.client = client;
            this.delegate = delegate;
            this.context = context;
            this.cluster = new HttpClientCloudCluster(context.getRegistry(), client);
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

        @Override
        public HttpResponse execute(final HttpHost target, final HttpRequest request, final HttpContext context) throws IOException {
            return doExecute(target, request, context);
        }

        @Override
        public HttpResponse execute(final HttpUriRequest request, final HttpContext context) throws IOException {
            Args.notNull(request, "HTTP request");
            return doExecute(determineTarget(request), request, context);
        }

        @Override
        public HttpResponse execute(final HttpUriRequest request) throws IOException {
            return execute(request, (HttpContext) null);
        }

        @Override
        public HttpResponse execute(final HttpHost target, final HttpRequest request) throws IOException {
            return doExecute(target, request, null);
        }

        @Override
        public <T> T execute(final HttpUriRequest request, final ResponseHandler<? extends T> responseHandler) throws IOException {
            return execute(request, responseHandler, null);
        }

        @Override
        public <T> T execute(final HttpUriRequest request, final ResponseHandler<? extends T> responseHandler, final HttpContext context) throws IOException {
            return execute(determineTarget(request), request, responseHandler, context);
        }

        @Override
        public <T> T execute(final HttpHost target, final HttpRequest request, final ResponseHandler<? extends T> responseHandler) throws IOException {
            return execute(target, request, responseHandler, null);
        }

        @Override
        public <T> T execute(final HttpHost target, final HttpRequest request, final ResponseHandler<? extends T> responseHandler, final HttpContext context) throws IOException {
            Args.notNull(responseHandler, "Response handler");
            HttpResponse response = execute(target, request, context);
            try {
                T result = responseHandler.handleResponse(response);
                HttpEntity entity = response.getEntity();
                EntityUtils.consume(entity);
                return result;
            } catch (final ClientProtocolException t) {
                // Try to salvage the underlying connection in case of a protocol exception
                final HttpEntity entity = response.getEntity();
                try {
                    EntityUtils.consume(entity);
                } catch (final Exception t2) {
                    // Log this exception. The original exception is more
                    // important and will be thrown to the caller.
                    logger.warn("Error consuming content after an exception.", t2);
                }
                throw t;
            }

        }

        private HttpResponse doExecute(HttpHost httpHost, HttpRequest request, HttpContext context) throws IOException {
            HttpClientClusterRequest clusterRequest = new HttpClientClusterRequest(request, context, delegate, cluster.getContext());
            OutboundInvocation.HttpOutboundInvocation<HttpClientClusterRequest> invocation = new OutboundInvocation.HttpOutboundInvocation<>(clusterRequest, this.context);
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

        private HttpHost determineTarget(final HttpUriRequest request) throws ClientProtocolException {
            // A null target may be acceptable if there is a default target.
            // Otherwise, the null target is detected in the director.
            HttpHost target = null;
            URI requestURI = request.getURI();
            if (requestURI.isAbsolute()) {
                target = URIUtils.extractHost(requestURI);
                if (target == null) {
                    throw new ClientProtocolException("URI does not specify a valid host name: " + requestURI);
                }
            }
            return target;
        }

        public void update() {
            delegateAccessor.set(client, this);
        }

    }
}
