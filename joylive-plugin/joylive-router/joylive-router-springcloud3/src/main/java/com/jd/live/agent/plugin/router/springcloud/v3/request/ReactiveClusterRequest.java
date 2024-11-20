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

import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.http.HttpUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFunction;

/**
 * Represents an outbound HTTP request in a reactive microservices architecture,
 * extending the capabilities of an abstract HTTP outbound request model to include
 * client-specific functionalities. This class encapsulates features such as load balancing,
 * service instance discovery, and lifecycle management, making it suitable for handling
 * dynamic client requests in a distributed system.
 */
public class ReactiveClusterRequest extends AbstractClusterRequest<ClientRequest> {

    private final ExchangeFunction next;

    /**
     * Creates a new instance of ReactiveClusterRequest.
     * @param request The ClientRequest object representing the original request.
     * @param loadBalancerFactory The ReactiveLoadBalancer.Factory object used to create the load balancer.
     * @param properties The LoadBalancerProperties object containing the configuration for the load balancer.
     * @param next The ExchangeFunction object representing the next step in the request processing chain.
     */
    public ReactiveClusterRequest(ClientRequest request,
                                  ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory,
                                  LoadBalancerProperties properties,
                                  ExchangeFunction next) {
        super(request, loadBalancerFactory, properties);
        this.uri = request.url();
        this.queries = new UnsafeLazyObject<>(() -> HttpUtils.parseQuery(request.url().getRawQuery()));
        this.headers = new UnsafeLazyObject<>(() -> HttpHeaders.writableHttpHeaders(request.headers()));
        this.cookies = new UnsafeLazyObject<>(request::cookies);
        this.next = next;
    }

    @Override
    public HttpMethod getHttpMethod() {
        try {
            return HttpMethod.valueOf(request.method().name());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    public ExchangeFunction getNext() {
        return next;
    }

    @Override
    protected RequestData buildRequestData() {
        return new RequestData(request);
    }
}
