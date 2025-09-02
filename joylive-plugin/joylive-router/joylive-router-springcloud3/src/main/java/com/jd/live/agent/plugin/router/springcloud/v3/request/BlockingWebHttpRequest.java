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
package com.jd.live.agent.plugin.router.springcloud.v3.request;

import com.jd.live.agent.core.util.io.UnsafeByteArrayOutputStream;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v3.cluster.BlockingWebCluster;
import com.jd.live.agent.plugin.router.springcloud.v3.response.BlockingClusterResponse;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpAccessor;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.jd.live.agent.core.util.http.HttpUtils.newURI;

/**
 * HTTP request implementation with integrated service discovery and governance policy enforcement.
 * Handles endpoint resolution through registry and executes requests via configured cluster strategy.
 */
public class BlockingWebHttpRequest implements ClientHttpRequest {

    private final URI uri;

    private final HttpMethod method;

    private final String service;

    private final HttpAccessor accessor;

    private final InvocationContext context;

    private final Registry registry;

    private final HttpHeaders headers = new HttpHeaders();

    private UnsafeByteArrayOutputStream outputStream;

    public BlockingWebHttpRequest(URI uri,
                                  HttpMethod method,
                                  String service,
                                  HttpAccessor accessor,
                                  InvocationContext context,
                                  Registry registry) {
        this.uri = uri;
        this.method = method;
        this.service = service;
        this.accessor = accessor;
        this.context = context;
        this.registry = registry;
        accessor.getClientHttpRequestInitializers().forEach(initializer -> initializer.initialize(this));
    }

    @Override
    @NonNull
    public ClientHttpResponse execute() throws IOException {
        try {
            List<ServiceEndpoint> endpoints = registry.subscribeAndGet(service, 5000, (message, e) -> new IOException(message, e));
            if (endpoints == null || endpoints.isEmpty()) {
                // Failed to convert microservice, fallback to domain reques
                return execute(uri);
            }
            return context.isFlowControlEnabled() ? request() : route();
        } catch (IOException | NestedRuntimeException e) {
            throw e;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw cause instanceof IOException ? (IOException) cause : new IOException(cause.getMessage(), cause);
        } catch (Throwable e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Directly executes request against a specific endpoint (bypasses discovery).
     *
     * @param endpoint Target server endpoint
     * @return HTTP response from specified endpoint
     * @throws IOException If request execution fails
     */
    public ClientHttpResponse execute(ServiceEndpoint endpoint) throws IOException {
        URI u = newURI(uri, endpoint.getHost(), endpoint.getPort());
        return execute(u);
    }

    @Override
    @NonNull
    public OutputStream getBody() throws IOException {
        if (outputStream == null) {
            outputStream = new UnsafeByteArrayOutputStream(1024);
        }
        return outputStream;
    }

    @Override
    @NonNull
    public String getMethodValue() {
        return method.name();
    }

    @Override
    @NonNull
    public URI getURI() {
        return uri;
    }

    @Override
    @NonNull
    public HttpHeaders getHeaders() {
        return headers;
    }

    /**
     * Executes the HTTP request with configured headers and body.
     *
     * @param uri the target URI for the request
     * @return the HTTP response
     * @throws IOException if an I/O error occurs during request execution
     */
    private ClientHttpResponse execute(URI uri) throws IOException {
        ClientHttpRequest request = accessor.getRequestFactory().createRequest(uri, method);
        request.getHeaders().putAll(headers);
        if (outputStream != null && outputStream.size() > 0) {
            outputStream.writeTo(request.getBody());
        }
        return request.execute();
    }

    /**
     * Executes request through cluster strategy with error handling conversion.
     *
     * @return Successful response from cluster-routed request
     * @throws Throwable Service-defined client errors or underlying exceptions
     * @see BlockingWebCluster  Cluster implementation handling load balancing/failover
     */
    private ClientHttpResponse request() throws Throwable {
        BlockingWebCluster cluster = BlockingWebCluster.INSTANCE;
        BlockingWebClusterRequest request = new BlockingWebClusterRequest(this, service, registry);
        HttpOutboundInvocation<BlockingWebClusterRequest> invocation = new HttpOutboundInvocation<>(request, context);
        BlockingClusterResponse response = cluster.request(invocation);
        ServiceError error = response.getError();
        if (error != null && !error.isServerError()) {
            throw error.getThrowable();
        } else {
            return response.getResponse();
        }
    }

    /**
     * Directly routes request to context-selected endpoint.
     *
     * @return Response from directly executed endpoint request
     * @throws Throwable Routing failures or execution errors
     * @see InvocationContext#route  Custom routing logic implementation
     */
    private ClientHttpResponse route() throws Throwable {
        BlockingCloudOutboundRequest request = new BlockingCloudOutboundRequest(this, service);
        HttpOutboundInvocation<BlockingCloudOutboundRequest> invocation = new HttpOutboundInvocation<>(request, context);
        ServiceEndpoint endpoint = context.route(invocation);
        return execute(endpoint);
    }
}
