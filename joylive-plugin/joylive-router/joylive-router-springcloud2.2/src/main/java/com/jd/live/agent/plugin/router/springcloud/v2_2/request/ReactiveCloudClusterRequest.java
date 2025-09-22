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
package com.jd.live.agent.plugin.router.springcloud.v2_2.request;

import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.plugin.router.springcloud.v2_2.cluster.context.ReactiveClusterContext;
import com.jd.live.agent.plugin.router.springcloud.v2_2.util.UriUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerClientRequestTransformer;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpHeaders.writableHttpHeaders;

/**
 * Represents an outbound HTTP request in a reactive microservices architecture,
 * extending the capabilities of an abstract HTTP outbound request model to include
 * client-specific functionalities. This class encapsulates features such as load balancing,
 * service instance discovery, and lifecycle management, making it suitable for handling
 * dynamic client requests in a distributed system.
 */
public class ReactiveCloudClusterRequest extends AbstractCloudClusterRequest<ClientRequest, ReactiveClusterContext> {

    private final ExchangeFunction next;

    public ReactiveCloudClusterRequest(ClientRequest request, ExchangeFunction next, ReactiveClusterContext context) {
        super(request, request.url(), context);
        this.next = next;
    }

    @Override
    public HttpMethod getHttpMethod() {
        org.springframework.http.HttpMethod method = request.method();
        return method == null ? null : HttpMethod.ofNullable(method.name());
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
            writableHttpHeaders(request.headers()).set(key, value);
        }
    }

    public ExchangeFunction getNext() {
        return next;
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return request.headers();
    }

    @Override
    protected Map<String, List<String>> parseCookies() {
        return request.cookies();
    }

    /**
     * Executes the HTTP request for a specific service instance and returns a reactive {@link ClientResponse}.
     *
     * @param instance the {@link ServiceInstance} to which the request is directed
     * @return a {@link Mono} emitting the {@link ClientResponse} containing the response data
     */
    public Mono<ClientResponse> exchange(ServiceInstance instance) {
        ClientRequest newRequest = createRequest(instance);
        return next.exchange(newRequest);
    }

    /**
     * Builds a new {@link ClientRequest} tailored for a specific {@link ServiceInstance}, incorporating sticky session
     * configurations and potential transformations.
     *
     * @param serviceInstance The {@link ServiceInstance} to which the request should be directed.
     * @return A new {@link ClientRequest} instance, modified to target the specified {@link ServiceInstance} and
     * potentially transformed by any configured {@link LoadBalancerClientRequestTransformer}s.
     */
    private ClientRequest createRequest(ServiceInstance serviceInstance) {
        URI originalUrl = request.url();
        ClientRequest result = ClientRequest
                .create(request.method(), UriUtils.newURI(serviceInstance, originalUrl))
                .headers(headers -> headers.addAll(request.headers()))
                .cookies(cookies -> {
                    cookies.addAll(request.cookies());
                })
                .attributes(attributes -> attributes.putAll(request.attributes()))
                .body(request.body())
                .build();
        List<LoadBalancerClientRequestTransformer> transformers = context.getTransformers();
        if (transformers != null) {
            for (LoadBalancerClientRequestTransformer transformer : transformers) {
                result = transformer.transformRequest(result, serviceInstance);
            }
        }
        return result;
    }
}
