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

import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.AbstractLiveCluster;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;
import com.jd.live.agent.plugin.router.springcloud.v3.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v3.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v3.request.SpringClusterRequest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMapAdapter;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Provides an abstract base for implementing client clusters that can send requests and receive responses from
 * various endpoints. This class serves as a foundation for managing a cluster of client endpoints and handling
 * common operations such as creating HTTP headers and exceptions.
 *
 * @param <R> the type of outbound requests the cluster handles
 * @param <O> the type of outbound responses the cluster can expect
 */
public abstract class AbstractClientCluster<
        R extends SpringClusterRequest,
        O extends OutboundResponse>
        extends AbstractLiveCluster<R, O, SpringEndpoint> {

    protected final SpringOutboundThrower<R> thrower = new SpringOutboundThrower<>();

    @Override
    public ClusterPolicy getDefaultPolicy(R request) {
        if (isRetryable()) {
            RetryPolicy retryPolicy = null;
            LoadBalancerProperties properties = request.getProperties();
            LoadBalancerProperties.Retry retry = properties == null ? null : properties.getRetry();
            if (retry != null && retry.isEnabled() && (request.getHttpMethod() == HttpMethod.GET || retry.isRetryOnAllOperations())) {
                Set<String> statuses = new HashSet<>(retry.getRetryableStatusCodes().size());
                retry.getRetryableStatusCodes().forEach(status -> statuses.add(String.valueOf(status)));
                retryPolicy = new RetryPolicy();
                retryPolicy.setRetry(retry.getMaxRetriesOnNextServiceInstance());
                retryPolicy.setInterval(retry.getBackoff().getMinBackoff().toMillis());
                retryPolicy.setErrorCodes(statuses);
            }
            return new ClusterPolicy(retryPolicy == null ? ClusterInvoker.TYPE_FAILFAST : ClusterInvoker.TYPE_FAILOVER, retryPolicy);
        }
        return new ClusterPolicy(ClusterInvoker.TYPE_FAILFAST);
    }

    /**
     * Determines if the current cluster support for retry.
     *
     * @return {@code true} if the operation is retryable; {@code false} otherwise.
     */
    protected abstract boolean isRetryable();

    /**
     * Discover the service instances for the requested service.
     *
     * @param request The outbound request to be routed.
     * @return ServiceInstance list
     */
    @Override
    public CompletionStage<List<SpringEndpoint>> route(R request) {
        CompletableFuture<List<SpringEndpoint>> future = new CompletableFuture<>();
        ServiceInstanceListSupplier supplier = request.getInstanceSupplier();
        if (supplier == null) {
            future.complete(new ArrayList<>());
        } else {
            Mono<List<ServiceInstance>> mono = supplier.get(request.getLbRequest()).next();
            mono.subscribe(
                    v -> {
                        List<SpringEndpoint> endpoints = new ArrayList<>();
                        if (v != null) {
                            v.forEach(i -> endpoints.add(new SpringEndpoint(i)));
                        }
                        future.complete(endpoints);
                    },
                    future::completeExceptionally
            );
        }
        return future;
    }

    @Override
    public Throwable createException(Throwable throwable, R request) {
        return thrower.createException(throwable, request);
    }

    @Override
    public Throwable createException(Throwable throwable, R request, SpringEndpoint endpoint) {
        return thrower.createException(throwable, request, endpoint);
    }

    @Override
    public Throwable createException(Throwable throwable, OutboundInvocation<R> invocation) {
        return thrower.createException(throwable, invocation);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onStart(R request) {
        request.lifecycles(l -> l.onStart(request.getLbRequest()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onDiscard(R request) {
        request.lifecycles(l -> l.onComplete(new CompletionContext<>(
                CompletionContext.Status.DISCARD, request.getLbRequest(), new EmptyResponse())));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onStartRequest(R request, SpringEndpoint endpoint) {
        request.lifecycles(l -> l.onStartRequest(request.getLbRequest(),
                endpoint == null ? new DefaultResponse(null) : endpoint.getResponse()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onError(Throwable throwable, R request, SpringEndpoint endpoint) {
        request.lifecycles(l -> l.onComplete(new CompletionContext<>(
                CompletionContext.Status.FAILED,
                throwable,
                request.getLbRequest(),
                endpoint == null ? new DefaultResponse(null) : endpoint.getResponse())));
    }

    @Override
    protected O createResponse(R request) {
        return createResponse(request, DegradeConfig.builder().responseCode(HttpStatus.OK.value()).responseBody("").build());
    }

    /**
     * Creates an {@link HttpHeaders} instance from a map of header names to lists of header values.
     * If the input map is {@code null}, this method returns an empty {@link HttpHeaders} instance.
     *
     * @param headers a map of header names to lists of header values
     * @return an {@link HttpHeaders} instance representing the provided headers
     */
    protected HttpHeaders getHttpHeaders(Map<String, List<String>> headers) {
        return headers == null ? new HttpHeaders() : new HttpHeaders(new MultiValueMapAdapter<>(headers));
    }

    protected boolean isUseRawStatusCodeInResponseData(LoadBalancerProperties properties) {
        boolean useRawStatusCodeInResponseData = false;
        if (properties != null) {
            // fix for spring cloud 2020
            try {
                useRawStatusCodeInResponseData = properties.isUseRawStatusCodeInResponseData();
            } catch (Throwable ignored) {
            }
        }
        return useRawStatusCodeInResponseData;
    }
}

