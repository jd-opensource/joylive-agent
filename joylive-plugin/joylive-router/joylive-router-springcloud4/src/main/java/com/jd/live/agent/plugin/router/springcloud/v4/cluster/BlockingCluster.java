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
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.plugin.router.springcloud.v4.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v4.request.BlockingClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.response.BlockingClusterResponse;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.core.util.type.ClassUtils.getValue;

/**
 * The {@code BlockingCluster} class extends {@code AbstractClientCluster} to provide a blocking
 * mechanism for handling HTTP requests, integrating load balancing and retry logic. It utilizes
 * Spring Cloud's load balancing capabilities to distribute requests across service instances and
 * supports configurable retry mechanisms for handling transient failures.
 * <p>
 * This class is designed to work within a microservices architecture, leveraging Spring Cloud's
 * infrastructure components to facilitate resilient and scalable service-to-service communication.
 *
 * @see AbstractClientCluster
 */
public class BlockingCluster extends AbstractClientCluster<BlockingClusterRequest, BlockingClusterResponse> {

    private static final Set<String> RETRY_EXCEPTIONS = new HashSet<>(Arrays.asList(
            "java.io.IOException",
            "java.util.concurrent.TimeoutException",
            "org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException"
    ));

    private static final ErrorPredicate RETRY_PREDICATE = new ErrorPredicate.DefaultErrorPredicate(null, RETRY_EXCEPTIONS);

    private static final String FIELD_REQUEST_FACTORY = "requestFactory";

    private static final String FIELD_LOAD_BALANCER_CLIENT_FACTORY = "loadBalancer.loadBalancerClientFactory";

    /**
     * An interceptor for HTTP requests, used to apply additional processing or modification
     * to requests before they are executed.
     */
    private final ClientHttpRequestInterceptor interceptor;

    /**
     * A factory for creating load-balanced {@code LoadBalancerRequest} instances, supporting
     * the dynamic selection of service instances based on load.
     */
    private final LoadBalancerRequestFactory requestFactory;

    /**
     * A factory for creating {@code ReactiveLoadBalancer} instances for service discovery
     * and load balancing.
     */
    private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

    /**
     * Constructs a {@code BlockingCluster} with the specified HTTP request interceptor.
     * Initializes the {@code requestFactory} and {@code loadBalancerFactory} fields by
     * reflectively accessing the interceptor's fields.
     *
     * @param interceptor the HTTP request interceptor to be used by this cluster
     */
    public BlockingCluster(ClientHttpRequestInterceptor interceptor) {
        this.interceptor = interceptor;
        this.requestFactory = getValue(interceptor, FIELD_REQUEST_FACTORY);
        this.loadBalancerFactory = getValue(interceptor, new String[]{FIELD_LOAD_BALANCER_CLIENT_FACTORY}, v -> v instanceof ReactiveLoadBalancer.Factory);
    }

    public ReactiveLoadBalancer.Factory<ServiceInstance> getLoadBalancerFactory() {
        return loadBalancerFactory;
    }

    @Override
    protected boolean isRetryable() {
        return interceptor instanceof RetryLoadBalancerInterceptor;
    }

    @Override
    public CompletionStage<BlockingClusterResponse> invoke(BlockingClusterRequest request, SpringEndpoint endpoint) {
        // TODO sticky session
        try {
            LoadBalancerRequest<ClientHttpResponse> lbRequest = requestFactory.createRequest(request.getRequest(), request.getBody(), request.getExecution());
            ClientHttpResponse response = lbRequest.apply(endpoint.getInstance());
            return CompletableFuture.completedFuture(new BlockingClusterResponse(response));
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
    public void onSuccess(BlockingClusterResponse response, BlockingClusterRequest request, SpringEndpoint endpoint) {
        try {
            ResponseData data = new ResponseData(response.getResponse(), new RequestData(request.getRequest()));
            request.lifecycles(l -> l.onComplete(new CompletionContext<>(
                    CompletionContext.Status.SUCCESS,
                    request.getLbRequest(),
                    endpoint.getResponse(),
                    data)));
        } catch (Throwable ignore) {
        }
    }

    @Override
    protected BlockingClusterResponse createResponse(BlockingClusterRequest httpRequest, DegradeConfig degradeConfig) {
        return new BlockingClusterResponse(new DegradeResponse(degradeConfig, httpRequest));
    }

    @Override
    protected BlockingClusterResponse createResponse(ServiceError error, ErrorPredicate predicate) {
        return new BlockingClusterResponse(error, predicate);
    }

    /**
     * A {@link ClientHttpResponse} implementation that uses {@link DegradeConfig} for response configuration.
     */
    private static class DegradeResponse implements ClientHttpResponse {
        private final DegradeConfig degradeConfig;
        private final BlockingClusterRequest httpRequest;
        private final int length;
        private final InputStream bodyStream;

        DegradeResponse(DegradeConfig degradeConfig, BlockingClusterRequest httpRequest) {
            this.degradeConfig = degradeConfig;
            this.httpRequest = httpRequest;
            String body = degradeConfig.getResponseBody();
            this.length = body == null ? 0 : body.length();
            this.bodyStream = body == null ? null : new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }

        @NonNull
        @Override
        public HttpStatus getStatusCode() throws IOException {
            return HttpStatus.valueOf(degradeConfig.getResponseCode());
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return degradeConfig.getResponseCode();
        }

        @NonNull
        @Override
        public String getStatusText() throws IOException {
            return "";
        }

        @Override
        public void close() {

        }

        @NonNull
        @Override
        public InputStream getBody() throws IOException {
            return bodyStream;
        }

        @NonNull
        @Override
        public HttpHeaders getHeaders() {
            HttpHeaders headers = new HttpHeaders();
            Map<String, List<String>> requestHeaders = httpRequest.getHeaders();
            if (requestHeaders != null) {
                headers.putAll(requestHeaders);
            }
            Map<String, String> attributes = degradeConfig.getAttributes();
            if (attributes != null) {
                attributes.forEach(headers::add);
            }
            headers.set(HttpHeaders.CONTENT_TYPE, degradeConfig.contentType());
            headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));
            return headers;
        }
    }
}
