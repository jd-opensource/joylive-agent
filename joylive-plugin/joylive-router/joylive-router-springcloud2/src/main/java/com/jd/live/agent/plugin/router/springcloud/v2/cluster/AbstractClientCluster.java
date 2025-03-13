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

import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.AbstractLiveCluster;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;
import com.jd.live.agent.plugin.router.springcloud.v2.cluster.context.CloudClusterContext;
import com.jd.live.agent.plugin.router.springcloud.v2.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v2.exception.StatusThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v2.exception.ThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v2.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v2.request.SpringClusterRequest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
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
        O extends OutboundResponse,
        C extends CloudClusterContext>
        extends AbstractLiveCluster<R, O, SpringEndpoint> {

    protected final C context;

    protected final SpringOutboundThrower<R> thrower;

    public AbstractClientCluster(C context) {
        this(context, StatusThrowerFactory.INSTANCE);
    }

    public AbstractClientCluster(C context, ThrowerFactory factory) {
        this.context = context;
        this.thrower = new SpringOutboundThrower<>(factory);
    }

    @Override
    public ClusterPolicy getDefaultPolicy(R request) {
        RetryPolicy retryPolicy = isRetryable() ? request.getDefaultRetryPolicy() : null;
        return new ClusterPolicy(retryPolicy == null ? ClusterInvoker.TYPE_FAILFAST : ClusterInvoker.TYPE_FAILOVER, retryPolicy);
    }

    /**
     * Retrieves the current context instance.
     *
     * @return the context instance of type {@code C}
     */
    public C getContext() {
        return context;
    }

    /**
     * Determines if the current context supports retry operations.
     *
     * @return {@code true} if the operation is retryable; {@code false} otherwise
     */
    public boolean isRetryable() {
        return context.isRetryable();
    }

    /**
     * Discover the service instances for the requested service.
     *
     * @param request The outbound request to be routed.
     * @return ServiceInstance list
     */
    @Override
    public CompletionStage<List<SpringEndpoint>> route(R request) {
        CompletableFuture<List<SpringEndpoint>> future = new CompletableFuture<>();
        Mono<List<ServiceInstance>> mono = request.getInstances();
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

    @Override
    protected O createResponse(R request) {
        return createResponse(request, DegradeConfig.builder().responseCode(HttpStatus.OK.value()).responseBody("").build());
    }
}

