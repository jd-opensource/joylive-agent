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
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ErrorPredicate.DefaultErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.plugin.router.springcloud.v2.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v2.request.FeignClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v2.response.FeignClusterResponse;
import com.jd.live.agent.plugin.router.springcloud.v2.util.LoadBalancerUtil;
import feign.Client;
import feign.Request;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.core.util.type.ClassUtils.getValue;

/**
 * A cluster implementation for Feign clients that manages a group of servers and provides load balancing and failover capabilities.
 *
 * @see AbstractClientCluster
 */
public class FeignCluster extends AbstractClientCluster<FeignClusterRequest, FeignClusterResponse> {

    private static final Set<String> RETRY_EXCEPTIONS = new HashSet<>(Arrays.asList(
            "java.io.IOException",
            "java.util.concurrent.TimeoutException",
            "org.springframework.cloud.client.loadbalancer.RetryableStatusCodeException"
    ));

    private static final ErrorPredicate RETRY_PREDICATE = new DefaultErrorPredicate(null, RETRY_EXCEPTIONS);

    private static final String FIELD_DELEGATE = "delegate";

    private static final String[] FIELD_CLIENT_FACTORIES = {"loadBalancerClient", "lbClientFactory"};

    private static final String[] FIELD_RETRY_PROPERTIES = {
            "loadBalancedRetryFactory.retryProperties",
            "lbClientFactory.loadBalancedRetryFactory.retryProperties"
    };

    private final Client client;

    private final Client delegate;

    private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

    public FeignCluster(Client client) {
        this.client = client;
        this.delegate = getValue(client, FIELD_DELEGATE);
        Object loadBalancerClient = getValue(client, FIELD_CLIENT_FACTORIES, null);
        this.loadBalancerFactory = LoadBalancerUtil.getFactory(loadBalancerClient);
        this.defaultRetryPolicy = createRetryPolicy(getValue(client, FIELD_RETRY_PROPERTIES, v -> v instanceof LoadBalancerRetryProperties));
    }

    public ReactiveLoadBalancer.Factory<ServiceInstance> getLoadBalancerFactory() {
        return loadBalancerFactory;
    }

    @Override
    public CompletionStage<FeignClusterResponse> invoke(FeignClusterRequest request, SpringEndpoint endpoint) {
        try {
            Request req = request.getRequest();
            String url = LoadBalancerUriTools.reconstructURI(endpoint.getInstance(), request.getURI()).toString();
            req = Request.create(req.httpMethod(), url, req.headers(), req.body(), req.charset(), req.requestTemplate());
            feign.Response response = delegate.execute(req, request.getOptions());
            return CompletableFuture.completedFuture(new FeignClusterResponse(response));
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @Override
    public ErrorPredicate getRetryPredicate() {
        return RETRY_PREDICATE;
    }

    @Override
    protected FeignClusterResponse createResponse(FeignClusterRequest request, DegradeConfig degradeConfig) {
        Request feignRequest = request.getRequest();
        String body = degradeConfig.getResponseBody();
        body = body == null ? "" : body;
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        Map<String, Collection<String>> headers = new HashMap<>(feignRequest.headers());
        if (degradeConfig.getAttributes() != null) {
            degradeConfig.getAttributes().forEach((k, v) -> headers.computeIfAbsent(k, k1 -> new ArrayList<>()).add(v));
        }
        headers.put(HttpHeaders.CONTENT_LENGTH, Collections.singletonList(String.valueOf(data.length)));
        headers.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(degradeConfig.contentType()));

        return new FeignClusterResponse(feign.Response.builder()
                .status(degradeConfig.getResponseCode())
                .body(data)
                .headers(headers)
                .request(feignRequest)
                .requestTemplate(feignRequest.requestTemplate())
                .build());
    }

    @Override
    protected FeignClusterResponse createResponse(ServiceError error, ErrorPredicate predicate) {
        return new FeignClusterResponse(error, predicate);
    }
}
