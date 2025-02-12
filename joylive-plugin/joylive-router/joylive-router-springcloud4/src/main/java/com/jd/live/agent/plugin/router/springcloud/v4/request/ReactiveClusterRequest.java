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
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import java.util.List;
import java.util.Map;

/**
 * Represents an outbound HTTP request in a reactive microservices architecture,
 * extending the capabilities of an abstract HTTP outbound request model to include
 * client-specific functionalities. This class encapsulates features such as load balancing,
 * service instance discovery, and lifecycle management, making it suitable for handling
 * dynamic client requests in a distributed system.
 */
public class ReactiveClusterRequest extends AbstractClusterRequest<ClientRequest> {

    private final ExchangeFunction next;

    private final HttpHeaders writeableHeaders;

    /**
     * Constructs a new ClientOutboundRequest with the specified parameters.
     *
     * @param request             The original client request to be processed.
     * @param loadBalancerFactory A factory for creating instances of ReactiveLoadBalancer for service instances.
     * @param next                The next processing step in the request handling pipeline.
     */
    public ReactiveClusterRequest(ClientRequest request,
                                  ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory,
                                  ExchangeFunction next) {
        super(request, request.url(), loadBalancerFactory, null);
        this.next = next;
        this.writeableHeaders = HttpHeaders.writableHttpHeaders(request.headers());
    }

    @Override
    public HttpMethod getHttpMethod() {
        try {
            return HttpMethod.valueOf(request.method().name());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    @Override
    public String getCookie(String key) {
        return key == null || key.isEmpty() ? null : request.cookies().getFirst(key);
    }

    @Override
    public String getHeader(String key) {
        return key == null || key.isEmpty() ? null : request.headers().getFirst(key);
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            writeableHeaders.set(key, value);
        }
    }

    public ExchangeFunction getNext() {
        return next;
    }

    @Override
    protected RequestData buildRequestData() {
        return new RequestData(request);
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return writeableHeaders;
    }

    @Override
    protected Map<String, List<String>> parseCookies() {
        return request.cookies();
    }
}
