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
package com.jd.live.agent.plugin.router.springcloud.v5.cluster;

import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.AbstractLiveCluster;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;
import com.jd.live.agent.plugin.router.springcloud.v5.cluster.context.CloudClusterContext;
import com.jd.live.agent.plugin.router.springcloud.v5.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v5.exception.ThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v5.exception.status.StatusThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v5.request.SpringClusterRequest;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Provides an abstract base for implementing client clusters that can send requests and receive responses from
 * various endpoints. This class serves as a foundation for managing a cluster of client endpoints and handling
 * common operations such as creating HTTP headers and exceptions.
 *
 * @param <R> the type of outbound requests the cluster handles
 * @param <O> the type of outbound responses the cluster can expect
 */
public abstract class AbstractCloudCluster<
        R extends SpringClusterRequest,
        O extends OutboundResponse,
        C extends CloudClusterContext>
        extends AbstractLiveCluster<R, O, ServiceEndpoint> {

    protected final C context;

    protected final SpringOutboundThrower<? extends NestedRuntimeException, R> thrower;

    public AbstractCloudCluster(C context) {
        this(context, new StatusThrowerFactory<>());
    }

    public AbstractCloudCluster(C context, ThrowerFactory<? extends NestedRuntimeException, R> factory) {
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

    @Override
    public CompletionStage<List<ServiceEndpoint>> route(R request) {
        return context.getEndpoints(request);
    }

    @Override
    public Throwable createException(Throwable throwable, R request) {
        return thrower.createException(throwable, request);
    }

    @Override
    public Throwable createException(Throwable throwable, R request, ServiceEndpoint endpoint) {
        return thrower.createException(throwable, request, endpoint);
    }

    @Override
    public Throwable createException(Throwable throwable, OutboundInvocation<R> invocation) {
        return thrower.createException(throwable, invocation);
    }

    @Override
    public void onStart(R request) {
        request.onStart();
    }

    @Override
    public void onDiscard(R request) {
        request.onDiscard();
    }

    @Override
    public void onStartRequest(R request, ServiceEndpoint endpoint) {
        request.onStartRequest(endpoint);
    }

    @Override
    public void onError(Throwable throwable, R request, ServiceEndpoint endpoint) {
        request.onError(throwable, endpoint);
    }

    @Override
    protected O createResponse(R request) {
        return createResponse(request, DegradeConfig.builder().responseCode(HttpStatus.OK.value()).responseBody("").build());
    }
}

