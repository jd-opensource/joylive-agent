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
import com.jd.live.agent.plugin.router.springcloud.v3.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v3.request.BlockingClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.response.BlockingClusterResponse;
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

import static com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException.getCircuitBreakException;

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

    private static final Logger logger = LoggerFactory.getLogger(BlockingCluster.class);

    private static final Set<String> RETRY_EXCEPTIONS = new HashSet<>(Arrays.asList(
            "java.io.IOException",
            "java.util.concurrent.TimeoutException",
            "org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException"
    ));

    private static final String FIELD_LOAD_BALANCER = "loadBalancer";

    private static final String FIELD_REQUEST_FACTORY = "requestFactory";

    private static final String FIELD_LOAD_BALANCER_CLIENT_FACTORY = "loadBalancerClientFactory";

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
    @SuppressWarnings("unchecked")
    public BlockingCluster(ClientHttpRequestInterceptor interceptor) {
        this.interceptor = interceptor;
        ClassDesc describe = ClassUtils.describe(interceptor.getClass());
        FieldList fieldList = describe.getFieldList();
        this.requestFactory = (LoadBalancerRequestFactory) fieldList.getField(FIELD_REQUEST_FACTORY).get(interceptor);
        LoadBalancerClient client = (LoadBalancerClient) fieldList.getField(FIELD_LOAD_BALANCER).get(interceptor);
        FieldDesc field = ClassUtils.describe(client.getClass()).getFieldList().getField(FIELD_LOAD_BALANCER_CLIENT_FACTORY);
        this.loadBalancerFactory = (ReactiveLoadBalancer.Factory<ServiceInstance>) field.get(client);
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
    public BlockingClusterResponse createResponse(Throwable throwable, BlockingClusterRequest request, SpringEndpoint endpoint) {
        RejectCircuitBreakException circuitBreakException = getCircuitBreakException(throwable);
        if (circuitBreakException != null) {
            DegradeConfig config = circuitBreakException.getConfig();
            if (config != null) {
                try {
                    return new BlockingClusterResponse(createResponse(request, config));
                } catch (Throwable e) {
                    logger.warn("Exception occurred when create degrade response from circuit break. caused by " + e.getMessage(), e);
                    return new BlockingClusterResponse(new ServiceError(createException(throwable, request, endpoint), false), null);
                }
            }
        }
        return new BlockingClusterResponse(new ServiceError(createException(throwable, request, endpoint), false), this::isRetryable);
    }

    @Override
    public boolean isRetryable(Throwable throwable) {
        return RetryPolicy.isRetry(RETRY_EXCEPTIONS, throwable);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(BlockingClusterResponse response, BlockingClusterRequest request, SpringEndpoint endpoint) {
        ClientHttpResponse httpResponse = response.getResponse();
        Object res = httpResponse;
        try {
            HttpHeaders responseHeaders = httpResponse.getHeaders();
            RequestData requestData = request.getRequestData();
            HttpStatus httpStatus = httpResponse.getStatusCode();
            int rawStatusCode = httpResponse.getRawStatusCode();
            LoadBalancerProperties properties = request.getProperties();
            boolean useRawStatusCodeInResponseData = properties != null && properties.isUseRawStatusCodeInResponseData();
            res = useRawStatusCodeInResponseData
                    ? new ResponseData(responseHeaders, null, requestData, rawStatusCode)
                    : new ResponseData(httpStatus, responseHeaders, null, requestData);
        } catch (Throwable ignore) {
        }

        Object clientResponse = res;
        request.lifecycles(l -> l.onComplete(new CompletionContext<>(
                CompletionContext.Status.SUCCESS,
                request.getLbRequest(),
                endpoint.getResponse(),
                clientResponse)));
    }

    /**
     * Creates a {@link ClientHttpResponse} based on the provided {@link BlockingClusterRequest} and {@link DegradeConfig}.
     * The response is configured with the status code, headers, and body specified in the degrade configuration.
     *
     * @param httpRequest   the original HTTP request containing headers.
     * @param degradeConfig the degrade configuration specifying the response details such as status code, headers, and body.
     * @return a {@link ClientHttpResponse} configured according to the degrade configuration.
     */
    private ClientHttpResponse createResponse(BlockingClusterRequest httpRequest, DegradeConfig degradeConfig) {
        return new DegradeResponse(degradeConfig, httpRequest);
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
            headers.set(HttpHeaders.CONTENT_TYPE, degradeConfig.getContentType());
            headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));
            return headers;
        }
    }
}
