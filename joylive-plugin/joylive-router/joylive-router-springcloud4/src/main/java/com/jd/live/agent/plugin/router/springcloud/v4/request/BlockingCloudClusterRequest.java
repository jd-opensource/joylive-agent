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
package com.jd.live.agent.plugin.router.springcloud.v4.request;

import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.plugin.router.springcloud.v4.cluster.context.BlockingClusterContext;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.util.List;
import java.util.Map;

/**
 * Represents a blocking request in a routing context, extending the capabilities of {@link AbstractCloudClusterRequest}
 * to handle HTTP requests in environments where reactive programming models are not used.
 *
 * @see AbstractCloudClusterRequest for the base functionality
 */
public class BlockingCloudClusterRequest extends AbstractCloudClusterRequest<HttpRequest, BlockingClusterContext> {

    /**
     * The body of the HTTP request.
     */
    private final byte[] body;

    /**
     * The execution context for the client HTTP request, allowing for further processing or manipulation.
     */
    private final ClientHttpRequestExecution execution;

    private final HttpHeaders writeableHeaders;

    public BlockingCloudClusterRequest(HttpRequest request,
                                       byte[] body,
                                       ClientHttpRequestExecution execution,
                                       BlockingClusterContext context) {
        super(request, request.getURI(), context);
        this.body = body;
        this.execution = execution;
        this.writeableHeaders = HttpHeaders.writableHttpHeaders(request.getHeaders());
    }

    @Override
    public HttpMethod getHttpMethod() {
        try {
            return HttpMethod.valueOf(request.getMethod().name());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    @Override
    public String getHeader(String key) {
        return key == null || key.isEmpty() ? null : request.getHeaders().getFirst(key);
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            writeableHeaders.set(key, value);
        }
    }

    @Override
    protected RequestData buildRequestData() {
        // cookie is used only in RequestBasedStickySessionServiceInstanceListSupplier
        // it's disabled by live interceptor
        // so we can use null value to improve performance.
        return new RequestData(request.getMethod(), request.getURI(), request.getHeaders(), null, null);
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return writeableHeaders;
    }

    /**
     * Executes the HTTP request for a specific service instance and returns the response.
     *
     * @param instance the {@link ServiceInstance} to which the request is directed
     * @return the {@link ClientHttpResponse} containing the response data
     * @throws Exception if an error occurs during the request execution
     */
    public ClientHttpResponse execute(ServiceInstance instance) throws Exception {
        LoadBalancerRequest<ClientHttpResponse> lbRequest = context.getRequestFactory().createRequest(request, body, execution);
        return lbRequest.apply(instance);
    }
}
