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
package com.jd.live.agent.plugin.router.springcloud.v2_1.cluster;

import com.jd.live.agent.core.util.cache.CacheObject;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.AbstractLiveCluster;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceRegistry;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;
import com.jd.live.agent.plugin.router.springcloud.v2_1.cluster.context.CloudClusterContext;
import com.jd.live.agent.plugin.router.springcloud.v2_1.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v2_1.exception.ThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v2_1.exception.status.StatusThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v2_1.instance.InstanceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v2_1.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v2_1.request.SpringClusterRequest;
import lombok.Getter;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

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
        extends AbstractLiveCluster<R, O, InstanceEndpoint> {

    protected static final Set<String> RETRY_EXCEPTIONS = new HashSet<>(Arrays.asList(
            "java.io.IOException",
            "java.util.concurrent.TimeoutException",
            "java.net.ConnectException",
            "java.net.SocketTimeoutException",
            "org.apache.http.conn.ConnectTimeoutException",
            "org.apache.http.NoHttpResponseException",
            "org.apache.http.conn.ConnectionPoolTimeoutException",
            "org.apache.http.ConnectionClosedException",
            "org.apache.http.conn.HttpHostConnectException"
    ));

    protected static final ErrorPredicate RETRY_PREDICATE = new ErrorPredicate.DefaultErrorPredicate(null, RETRY_EXCEPTIONS);

    @Getter
    protected final C context;

    protected final Map<String, CacheObject<RetryPolicy>> retryPolicies = new ConcurrentHashMap<>();

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
        CacheObject<RetryPolicy> cacheObject = !isRetryable()
                ? null
                : retryPolicies.computeIfAbsent(request.getService(), s -> new CacheObject<>(context.getDefaultRetryPolicy(s)));
        RetryPolicy retryPolicy = cacheObject == null ? null : cacheObject.get();
        return new ClusterPolicy(retryPolicy == null ? ClusterInvoker.TYPE_FAILFAST : ClusterInvoker.TYPE_FAILOVER, retryPolicy);
    }

    @Override
    public ErrorPredicate getRetryPredicate() {
        return RETRY_PREDICATE;
    }

    /**
     * Determines if the current context supports retry operations.
     *
     * @return {@code true} if the operation is retryable; {@code false} otherwise
     */
    public boolean isRetryable() {
        return context.isRetryable();
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletionStage<List<InstanceEndpoint>> route(R request) {
        ServiceRegistry registry = context.getServiceRegistry(request.getService());
        List<ServiceEndpoint> endpoints = registry.getEndpoints();
        if (endpoints == null || endpoints.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        } else if (endpoints.get(0) instanceof InstanceEndpoint) {
            return CompletableFuture.completedFuture((List) endpoints);
        } else {
            List<InstanceEndpoint> instances = toList(endpoints, e -> new SpringEndpoint(request.getService(), e));
            return CompletableFuture.completedFuture(instances);
        }
    }

    @Override
    public Throwable createException(Throwable throwable, R request) {
        return thrower.createException(throwable, request);
    }

    @Override
    public Throwable createException(Throwable throwable, R request, InstanceEndpoint endpoint) {
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

