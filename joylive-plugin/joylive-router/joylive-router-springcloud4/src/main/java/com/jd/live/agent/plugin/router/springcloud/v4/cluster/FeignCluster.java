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

import com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.type.ClassDesc;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.core.util.type.FieldList;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.response.ServiceError;
import com.jd.live.agent.plugin.router.springcloud.v4.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v4.request.FeignClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.response.FeignClusterResponse;
import feign.Client;
import feign.Request;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.loadbalancer.RetryableFeignBlockingLoadBalancerClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException.getCircuitBreakException;

/**
 * A client cluster implementation for Feign-based service calls.
 */
public class FeignCluster extends AbstractClientCluster<FeignClusterRequest, FeignClusterResponse> {

    private static final Logger logger = LoggerFactory.getLogger(FeignCluster.class);

    private static final Set<String> RETRY_EXCEPTIONS = new HashSet<>(Arrays.asList(
            "java.io.IOException",
            "java.util.concurrent.TimeoutException",
            "org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException"
    ));

    private static final String FIELD_DELEGATE = "delegate";

    private static final String FIELD_LOAD_BALANCER_CLIENT_FACTORY = "loadBalancerClientFactory";

    private final Client client;

    private final Client delegate;

    private final LoadBalancerClientFactory loadBalancerClientFactory;

    public FeignCluster(Client client) {
        this.client = client;
        ClassDesc describe = ClassUtils.describe(client.getClass());
        FieldList fieldList = describe.getFieldList();
        FieldDesc field = fieldList.getField(FIELD_DELEGATE);
        this.delegate = (Client) (field == null ? null : field.get(client));
        field = fieldList.getField(FIELD_LOAD_BALANCER_CLIENT_FACTORY);
        this.loadBalancerClientFactory = (LoadBalancerClientFactory) (field == null ? null : field.get(client));
    }

    public LoadBalancerClientFactory getLoadBalancerClientFactory() {
        return loadBalancerClientFactory;
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
    public FeignClusterResponse createResponse(Throwable throwable, FeignClusterRequest request, SpringEndpoint endpoint) {
        if (throwable == null) {
            return new FeignClusterResponse(createResponse(request,
                    DegradeConfig.builder().responseCode(HttpStatus.OK.value()).responseBody("").build()));
        }
        RejectCircuitBreakException circuitBreakException = getCircuitBreakException(throwable);
        if (circuitBreakException != null) {
            DegradeConfig config = circuitBreakException.getConfig();
            if (config != null) {
                try {
                    return new FeignClusterResponse(createResponse(request, config));
                } catch (Throwable e) {
                    logger.warn("Exception occurred when create degrade response from circuit break. caused by " + e.getMessage(), e);
                    return new FeignClusterResponse(new ServiceError(createException(throwable, request, endpoint), false), null);
                }
            }
        }
        return new FeignClusterResponse(new ServiceError(createException(throwable, request, endpoint), false), this::isRetryable);
    }

    @Override
    public boolean isRetryable(Throwable throwable) {
        // TODO modify isRetryable
        return RetryPolicy.isRetry(RETRY_EXCEPTIONS, throwable);
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

    /**
     * Creates a {@link feign.Response} based on the provided {@link FeignClusterRequest} and {@link DegradeConfig}.
     * The response is configured with the status code, headers, and body specified in the degrade configuration.
     *
     * @param request       the original HTTP request containing headers.
     * @param degradeConfig the degrade configuration specifying the response details such as status code, headers, and body.
     * @return a {@link feign.Response} configured according to the degrade configuration.
     */
    private feign.Response createResponse(FeignClusterRequest request, DegradeConfig degradeConfig) {
        Request feignRequest = request.getRequest();
        String body = degradeConfig.getResponseBody();
        body = body == null ? "" : body;
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        Map<String, Collection<String>> headers = new HashMap<>(feignRequest.headers());
        if (degradeConfig.getAttributes() != null) {
            degradeConfig.getAttributes().forEach((k, v) -> headers.computeIfAbsent(k, k1 -> new ArrayList<>()).add(v));
        }
        headers.put(HttpHeaders.CONTENT_LENGTH, Collections.singletonList(String.valueOf(data.length)));
        headers.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(degradeConfig.getContentType()));

        return feign.Response.builder()
                .status(degradeConfig.getResponseCode())
                .body(data)
                .headers(headers)
                .request(feignRequest)
                .requestTemplate(feignRequest.requestTemplate())
                .build();
    }
}
