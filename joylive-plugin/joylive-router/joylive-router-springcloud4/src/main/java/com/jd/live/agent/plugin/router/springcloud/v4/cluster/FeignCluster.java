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
package com.jd.live.agent.plugin.router.springcloud.v4.cluster;

import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ErrorPredicate.DefaultErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.plugin.router.springcloud.v4.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v4.request.FeignClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.response.FeignClusterResponse;
import feign.Client;
import feign.Request;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.openfeign.loadbalancer.RetryableFeignBlockingLoadBalancerClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.core.util.type.ClassUtils.getValue;

/**
 * A client cluster implementation for Feign-based service calls.
 */
public class FeignCluster extends AbstractClientCluster<FeignClusterRequest, FeignClusterResponse> {

    private static final Set<String> RETRY_EXCEPTIONS = new HashSet<>(Arrays.asList(
            "java.io.IOException",
            "java.util.concurrent.TimeoutException",
            "org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException"
    ));

    private static final ErrorPredicate RETRY_PREDICATE = new DefaultErrorPredicate(null, RETRY_EXCEPTIONS);

    private static final String FIELD_DELEGATE = "delegate";

    private static final String FIELD_LOAD_BALANCER_CLIENT_FACTORY = "loadBalancerClientFactory";

    private final Client client;

    private final Client delegate;

    private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

    public FeignCluster(Client client) {
        this.client = client;
        this.delegate = getValue(client, FIELD_DELEGATE);
        this.loadBalancerFactory = getValue(client, FIELD_LOAD_BALANCER_CLIENT_FACTORY);
    }

    public ReactiveLoadBalancer.Factory<ServiceInstance> getLoadBalancerFactory() {
        return loadBalancerFactory;
    }

    @Override
    protected boolean isRetryable() {
        return client instanceof RetryableFeignBlockingLoadBalancerClient;
    }

    @Override
    public CompletionStage<FeignClusterResponse> invoke(FeignClusterRequest request, SpringEndpoint endpoint) {
        try {
            Request req = request.getRequest();
            String url = LoadBalancerUriTools.reconstructURI(endpoint.getInstance(), request.getURI()).toString();
            // TODO sticky session
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

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(FeignClusterResponse response, FeignClusterRequest request, SpringEndpoint endpoint) {
        try {
            HttpHeaders responseHeaders = getHttpHeaders(response.getHeaders());
            RequestData requestData = request.getRequestData();
            HttpStatus httpStatus = HttpStatus.resolve(response.getResponse().status());
            request.lifecycles(l -> l.onComplete(new CompletionContext<>(
                    CompletionContext.Status.SUCCESS,
                    request.getLbRequest(),
                    endpoint.getResponse(),
                    new ResponseData(httpStatus, responseHeaders, null, requestData))));
        } catch (Throwable ignore) {
        }
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
