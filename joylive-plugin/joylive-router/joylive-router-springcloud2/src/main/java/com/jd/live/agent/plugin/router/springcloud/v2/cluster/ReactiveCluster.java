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
package com.jd.live.agent.plugin.router.springcloud.v2.cluster;

import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.plugin.router.springcloud.v2.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v2.request.ReactiveClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v2.response.ReactiveClusterResponse;
import com.jd.live.agent.plugin.router.springcloud.v2.util.LoadBalancerUtil;
import com.jd.live.agent.plugin.router.springcloud.v2.util.UriUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerClientRequestTransformer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.core.util.type.ClassUtils.getValue;

/**
 * @author: yuanjinzhong
 * @date: 2025/1/3 17:43
 * @description:  a cluster for reactor mode
 * @see  org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction
 */
public class ReactiveCluster extends AbstractClientCluster<ReactiveClusterRequest, ReactiveClusterResponse> {


    private static final Set<String> RETRY_EXCEPTIONS = new HashSet<>(Arrays.asList(
            "java.io.IOException",
            "java.util.concurrent.TimeoutException",
            "org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException"
    ));

    private static final ErrorPredicate RETRY_PREDICATE = new ErrorPredicate.DefaultErrorPredicate(null, RETRY_EXCEPTIONS);

    private static final String FIELD_LOAD_BALANCER = "loadBalancerClient";

    private static final String FIELD_LOAD_BALANCER_FACTORY = "loadBalancerFactory";

    private static final String FIELD_TRANSFORMERS = "transformers";

    private final ExchangeFilterFunction filterFunction;

    private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

    private final List<LoadBalancerClientRequestTransformer> transformers;

    public ReactiveCluster(ExchangeFilterFunction exchangeFilterFunction) {
        LoadBalancerClient client = getValue(exchangeFilterFunction, FIELD_LOAD_BALANCER);
        this.filterFunction = exchangeFilterFunction;
        /*
          If client is not null, it indicates that the currently intercepted class is LoadBalancerExchangeFilterFunction;
          otherwise, the intercepted class is ReactorLoadBalancerExchangeFilterFunction.
         */
        this.loadBalancerFactory = client != null ? LoadBalancerUtil.getFactory(client) : getValue(filterFunction, FIELD_LOAD_BALANCER_FACTORY);
        this.transformers = getValue(filterFunction, FIELD_TRANSFORMERS);
    }

    public ReactiveLoadBalancer.Factory<ServiceInstance> getLoadBalancerFactory() {
        return loadBalancerFactory;
    }

    @Override
    public ErrorPredicate getRetryPredicate() {
        return RETRY_PREDICATE;
    }

    @Override
    public CompletionStage<ReactiveClusterResponse> invoke(ReactiveClusterRequest request, SpringEndpoint endpoint) {
        try {
            ClientRequest newRequest = buildRequest(request, endpoint.getInstance());
            return request.getNext().exchange(newRequest).map(ReactiveClusterResponse::new).toFuture();
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    /**
     * Builds a new {@link ClientRequest} tailored for a specific {@link ServiceInstance}, incorporating sticky session
     * configurations and potential transformations.
     *
     * @param request         The original {@link ReactiveClusterRequest} containing the request to be sent and its associated
     *                        load balancer properties.
     * @param serviceInstance The {@link ServiceInstance} to which the request should be directed.
     * @return A new {@link ClientRequest} instance, modified to target the specified {@link ServiceInstance} and
     * potentially transformed by any configured {@link LoadBalancerClientRequestTransformer}s.
     */
    private ClientRequest buildRequest(ReactiveClusterRequest request, ServiceInstance serviceInstance) {
        ClientRequest clientRequest = request.getRequest();
        URI originalUrl = clientRequest.url();
        ClientRequest result = ClientRequest
                .create(clientRequest.method(), UriUtils.newURI(serviceInstance, originalUrl))
                .headers(headers -> headers.addAll(clientRequest.headers()))
                .cookies(cookies -> {
                    cookies.addAll(clientRequest.cookies());
                })
                .attributes(attributes -> attributes.putAll(clientRequest.attributes()))
                .body(clientRequest.body())
                .build();
        if (transformers != null) {
            for (LoadBalancerClientRequestTransformer transformer : transformers) {
                result = transformer.transformRequest(result, serviceInstance);
            }
        }
        return result;
    }

    @Override
    protected ReactiveClusterResponse createResponse(ReactiveClusterRequest request, DegradeConfig degradeConfig) {
        ExchangeStrategies strategies;
        try {
            FieldDesc field = ClassUtils.describe(request.getNext().getClass()).getFieldList().getField("strategies");
            strategies = field == null ? ExchangeStrategies.withDefaults() : (ExchangeStrategies) field.get(request.getNext());
        } catch (Throwable ignored) {
            strategies = ExchangeStrategies.withDefaults();
        }
        int length = degradeConfig.getBodyLength();
        return new ReactiveClusterResponse(ClientResponse.create(degradeConfig.getResponseCode(), strategies)
                .body(length == 0 ? "" : degradeConfig.getResponseBody())
                .request(new DegradeHttpRequest(request))
                .headers(headers -> {
                    headers.addAll(request.getRequest().headers());
                    degradeConfig.foreach(headers::add);
                    headers.set(HttpHeaders.CONTENT_TYPE, degradeConfig.getContentType());
                    headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));
                }).build());
    }

    @Override
    protected ReactiveClusterResponse createResponse(ServiceError error, ErrorPredicate predicate) {
        return new ReactiveClusterResponse(error, predicate);
    }

    /**
     * A class that implements the HttpRequest interface and wrap a ReactiveClusterRequest.
     */
    private static class DegradeHttpRequest implements HttpRequest {

        private final ReactiveClusterRequest request;

        DegradeHttpRequest(ReactiveClusterRequest request) {
            this.request = request;
        }

        @Override
        @NonNull
        public HttpMethod getMethod() {
            return request.getRequest().method();
        }

        @Override
        public String getMethodValue() {
            return request.getRequest().method().name();
        }

        @Override
        @NonNull
        public URI getURI() {
            return request.getRequest().url();
        }

        @Override
        @NonNull
        public HttpHeaders getHeaders() {
            return request.getRequest().headers();
        }
    }
}

