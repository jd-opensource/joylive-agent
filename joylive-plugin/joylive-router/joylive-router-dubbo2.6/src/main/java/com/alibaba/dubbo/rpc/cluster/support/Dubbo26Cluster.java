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
package com.alibaba.dubbo.rpc.cluster.support;


import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.support.RpcUtils;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.type.ClassDesc;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.core.util.type.FieldList;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.AbstractLiveCluster;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.invoke.cluster.LiveCluster;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ErrorPredicate.DefaultErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.plugin.router.dubbo.v2_6.exception.Dubbo26OutboundThrower;
import com.jd.live.agent.plugin.router.dubbo.v2_6.instance.DubboEndpoint;
import com.jd.live.agent.plugin.router.dubbo.v2_6.request.DubboRequest.DubboOutboundRequest;
import com.jd.live.agent.plugin.router.dubbo.v2_6.response.DubboResponse.DubboOutboundResponse;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


/**
 * Implements the {@link LiveCluster} interface for handling Dubbo outbound requests,
 * responses, and routing them to Dubbo endpoints. This class is specific to Dubbo's
 * RPC mechanism and provides the functionality to manage sticky sessions, route
 * requests, and handle invocation logic.
 * <p>
 * The class wraps around an {@link AbstractClusterInvoker} to leverage Dubbo's
 * clustering mechanism for routing and invoking RPC requests.
 * </p>
 */
public class Dubbo26Cluster extends AbstractLiveCluster<DubboOutboundRequest, DubboOutboundResponse, DubboEndpoint<?>> {

    private static final ErrorPredicate RETRY_PREDICATE = new DefaultErrorPredicate(
            throwable -> throwable instanceof RpcException && (
                    (((RpcException) throwable)).isNetwork()
                            || (((RpcException) throwable)).isTimeout()), null);

    private final AbstractClusterInvoker cluster;

    private final ObjectParser parser;

    private final Dubbo26OutboundThrower thrower;

    private final AtomicBoolean destroyed;

    /**
     * The identifier used for stickiness. This ID is used to route requests to
     * the same provider consistently.
     */
    private String stickyId;

    public Dubbo26Cluster(AbstractClusterInvoker cluster, ObjectParser parser) {
        this.cluster = cluster;
        this.parser = parser;
        this.thrower = new Dubbo26OutboundThrower(cluster);
        ClassDesc describe = ClassUtils.describe(cluster.getClass());
        FieldList fieldList = describe.getFieldList();
        FieldDesc field = fieldList.getField("destroyed");
        this.destroyed = (AtomicBoolean) (field == null ? null : field.get(cluster));
    }

    @Override
    public String getStickyId() {
        return stickyId;
    }

    @Override
    public void setStickyId(String stickyId) {
        this.stickyId = stickyId;
    }

    @Override
    public ClusterPolicy getDefaultPolicy(DubboOutboundRequest request) {
        ClusterPolicy policy = new ClusterPolicy();
        if (cluster instanceof FailoverClusterInvoker) {
            // no retry interval in com.alibaba.dubbo.rpc.cluster.support.FailoverClusterInvoker
            RetryPolicy retryPolicy = new RetryPolicy();
            retryPolicy.setRetry(getRetries(RpcUtils.getMethodName(request.getRequest())));
            policy.setType(ClusterInvoker.TYPE_FAILOVER);
            policy.setRetryPolicy(retryPolicy);
        } else if (cluster instanceof FailfastClusterInvoker) {
            policy.setType(ClusterInvoker.TYPE_FAILFAST);
        } else if (cluster instanceof FailsafeClusterInvoker) {
            policy.setType(ClusterInvoker.TYPE_FAILSAFE);
        } else if (cluster instanceof FailbackClusterInvoker) {
            policy.setType(ClusterInvoker.TYPE_FAILFAST);
        } else if (cluster instanceof BroadcastClusterInvoker) {
            policy.setType(null);
        } else if (cluster instanceof ForkingClusterInvoker) {
            policy.setType(ClusterInvoker.TYPE_FAILFAST);
        } else {
            policy.setType(ClusterInvoker.TYPE_FAILFAST);
        }
        return policy;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletionStage<List<DubboEndpoint<?>>> route(DubboOutboundRequest request) {
        try {
            List<Invoker<?>> invokers = cluster.list(request.getRequest());
            return CompletableFuture.completedFuture(invokers == null
                    ? new ArrayList<>()
                    : invokers.stream().map(DubboEndpoint::of).collect(Collectors.toList()));
        } catch (RpcException e) {
            return Futures.future(e);
        }
    }

    @Override
    public CompletionStage<DubboOutboundResponse> invoke(DubboOutboundRequest request, DubboEndpoint<?> endpoint) {
        try {
            Result result = endpoint.getInvoker().invoke(request.getRequest());
            DubboOutboundResponse response = new DubboOutboundResponse(result, getRetryPredicate());
            return CompletableFuture.completedFuture(response);
        } catch (Throwable e) {
            return CompletableFuture.completedFuture(new DubboOutboundResponse(new ServiceError(e, false), getRetryPredicate()));
        }
    }

    @Override
    public ErrorPredicate getRetryPredicate() {
        return RETRY_PREDICATE;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed != null && destroyed.get();
    }

    @Override
    public Throwable createException(Throwable throwable, DubboOutboundRequest request) {
        return thrower.createException(throwable, request);
    }

    @Override
    public Throwable createException(Throwable throwable, DubboOutboundRequest request, DubboEndpoint<?> endpoint) {
        return thrower.createException(throwable, request, endpoint);
    }

    @Override
    public Throwable createException(Throwable throwable, OutboundInvocation<DubboOutboundRequest> invocation) {
        return thrower.createException(throwable, invocation);
    }

    /**
     * Retrieves the configured number of retries for a specific method invocation.
     *
     * @param methodName The name of the method for which the retry count is to be retrieved.
     * @return The number of retries configured for the specified method, defaulting to 1 if the
     * configured value is less than or equal to 0.
     */
    private int getRetries(String methodName) {
        int len = cluster.getUrl().getMethodParameter(methodName, Constants.RETRIES_KEY, Constants.DEFAULT_RETRIES);
        if (len <= 0) {
            len = 1;
        }
        return len;
    }

    @Override
    protected DubboOutboundResponse createResponse(DubboOutboundRequest request) {
        return new DubboOutboundResponse(new RpcResult());
    }

    @Override
    protected DubboOutboundResponse createResponse(DubboOutboundRequest request, DegradeConfig degradeConfig) {
        RpcInvocation invocation = (RpcInvocation) request.getRequest();
        String body = degradeConfig.getResponseBody();
        RpcResult result = new RpcResult();
        result.setAttachments(degradeConfig.getAttributes());
        if (body != null) {
            Object value;
            if (request.isGeneric()) {
                value = degradeConfig.text()
                        ? body
                        : parser.read(new StringReader(body), request.loadClass(degradeConfig.getContentType(), Object.class));
            } else {
                Type[] types = RpcUtils.getReturnTypes(invocation);
                if (types == null || types.length == 0) {
                    // void return
                    value = null;
                } else if (types.length == 1) {
                    Class<?> type = (Class<?>) types[0];
                    value = String.class == type ? body : parser.read(new StringReader(body), type);
                } else {
                    value = parser.read(new StringReader(body), types[1]);
                }
            }
            result.setValue(value);
        }

        return new DubboOutboundResponse(result);
    }

    @Override
    protected DubboOutboundResponse createResponse(ServiceError error, ErrorPredicate predicate) {
        return new DubboOutboundResponse(error, predicate);
    }

}

