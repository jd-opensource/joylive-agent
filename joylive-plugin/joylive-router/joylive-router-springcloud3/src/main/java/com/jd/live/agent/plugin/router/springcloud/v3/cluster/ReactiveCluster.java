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
package com.jd.live.agent.plugin.router.springcloud.v3.cluster;

import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.plugin.router.springcloud.v3.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v3.request.ReactiveClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.response.ReactiveClusterResponse;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerClientRequestTransformer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.RetryableLoadBalancerExchangeFilterFunction;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.core.util.type.ClassUtils.getValue;

/**
 * Represents a client cluster that handles outbound requests and responses for services
 * within a microservices architecture, utilizing a reactive load balancer. This class
 * integrates with Spring's WebClient and load balancing infrastructure to dynamically
 * select service instances based on load balancing strategies and policies.
 *
 * <p>This class is designed to work with {@link ReactiveClusterRequest} and
 * {@link ReactiveClusterResponse}, facilitating the routing and invocation of requests
 * to services identified by {@link SpringEndpoint}s and handling exceptions with
 * {@link NestedRuntimeException}.
 *
 * <p>It supports retry mechanisms based on configurable exceptions and integrates
 * service instance selection with load balancing and retry policies.
 */
public class ReactiveCluster extends AbstractClientCluster<ReactiveClusterRequest, ReactiveClusterResponse> {

    private static final Set<String> RETRY_EXCEPTIONS = new HashSet<>(Arrays.asList(
            "java.io.IOException",
            "java.util.concurrent.TimeoutException",
            "org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException"
    ));

    private static final ErrorPredicate RETRY_PREDICATE = new ErrorPredicate.DefaultErrorPredicate(null, RETRY_EXCEPTIONS);

    private static final String FIELD_LOAD_BALANCER_FACTORY = "loadBalancerFactory";

    private static final String FIELD_PROPERTIES = "properties";

    private static final String FIELD_TRANSFORMERS = "transformers";

    private final LoadBalancedExchangeFilterFunction filterFunction;

    private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

    private final LoadBalancerProperties loadBalancerProperties;

    private final List<LoadBalancerClientRequestTransformer> transformers;

    /**
     * Constructs a new ClientCluster with the specified {@link LoadBalancedExchangeFilterFunction}.
     * This constructor initializes the load balancer factory and transformers by reflecting
     * on the provided filterFunction's class fields.
     *
     * @param filterFunction The {@link LoadBalancedExchangeFilterFunction} used to filter exchange functions.
     */
    public ReactiveCluster(LoadBalancedExchangeFilterFunction filterFunction) {
        this.filterFunction = filterFunction;
        this.loadBalancerFactory = getValue(filterFunction, FIELD_LOAD_BALANCER_FACTORY);
        this.loadBalancerProperties = getValue(loadBalancerFactory, FIELD_PROPERTIES, v -> v instanceof LoadBalancerProperties);
        this.transformers = getValue(filterFunction, FIELD_TRANSFORMERS);
    }

    public ReactiveLoadBalancer.Factory<ServiceInstance> getLoadBalancerFactory() {
        return loadBalancerFactory;
    }

    public LoadBalancerProperties getLoadBalancerProperties() {
        return loadBalancerProperties;
    }

    @Override
    protected boolean isRetryable() {
        return filterFunction instanceof RetryableLoadBalancerExchangeFilterFunction;
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

    @Override
    public ErrorPredicate getRetryPredicate() {
        return RETRY_PREDICATE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ReactiveClusterResponse response, ReactiveClusterRequest request, SpringEndpoint endpoint) {
        boolean useRawStatusCodeInResponseData = isUseRawStatusCodeInResponseData(request.getProperties());
        request.lifecycles(l -> l.onComplete(new CompletionContext<>(
                CompletionContext.Status.SUCCESS,
                request.getLbRequest(),
                endpoint.getResponse(),
                useRawStatusCodeInResponseData
                        ? new ResponseData(new RequestData(request.getRequest()), response.getResponse())
                        : new ResponseData(response.getResponse(), new RequestData(request.getRequest())))));
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
        LoadBalancerProperties properties = request.getProperties();
        LoadBalancerProperties.StickySession stickySession = properties == null ? null : properties.getStickySession();
        String instanceIdCookieName = stickySession == null ? null : stickySession.getInstanceIdCookieName();
        boolean addServiceInstanceCookie = stickySession != null && stickySession.isAddServiceInstanceCookie();
        ClientRequest clientRequest = request.getRequest();
        URI originalUrl = clientRequest.url();
        ClientRequest result = ClientRequest
                .create(clientRequest.method(), LoadBalancerUriTools.reconstructURI(serviceInstance, originalUrl))
                .headers(headers -> headers.addAll(clientRequest.headers()))
                .cookies(cookies -> {
                    cookies.addAll(clientRequest.cookies());
                    // todo how to use this sticky session
                    if (!(instanceIdCookieName == null || instanceIdCookieName.isEmpty()) && addServiceInstanceCookie) {
                        cookies.add(instanceIdCookieName, serviceInstance.getInstanceId());
                    }
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
        return new ReactiveClusterResponse(ClientResponse.create(degradeConfig.getResponseCode(), strategies)
                .body(degradeConfig.getResponseBody() == null ? "" : degradeConfig.getResponseBody())
                .request(new DegradeHttpRequest(request))
                .headers(headers -> {
                    headers.addAll(request.getRequest().headers());
                    degradeConfig.foreach(headers::add);
                    headers.set(HttpHeaders.CONTENT_TYPE, degradeConfig.contentType());
                    headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(degradeConfig.bodyLength()));
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
