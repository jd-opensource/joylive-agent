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
package org.apache.dubbo.rpc.cluster.support.v3;


import com.alibaba.dubbo.rpc.support.RpcUtils;
import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.governance.exception.RetryExhaustedException;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.invoke.cluster.LiveCluster;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.response.Response;
import com.jd.live.agent.plugin.router.dubbo.v3.instance.DubboEndpoint;
import com.jd.live.agent.plugin.router.dubbo.v3.request.DubboRequest.DubboOutboundRequest;
import com.jd.live.agent.plugin.router.dubbo.v3.response.DubboResponse.DubboOutboundResponse;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.support.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_RETRIES;
import static org.apache.dubbo.common.constants.CommonConstants.RETRIES_KEY;


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
public class DubboCluster implements LiveCluster<DubboOutboundRequest, DubboOutboundResponse, DubboEndpoint<?>, RpcException> {

    private final AbstractClusterInvoker cluster;

    /**
     * The identifier used for stickiness. This ID is used to route requests to
     * the same provider consistently.
     */
    private String stickyId;

    /**
     * Constructs a new LiveCluster that wraps an abstract cluster.
     *
     * @param cluster the abstract cluster to be wrapped by this live cluster
     */
    public DubboCluster(AbstractClusterInvoker cluster) {
        this.cluster = cluster;
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
            RetryPolicy retryPolicy = new RetryPolicy();
            retryPolicy.setRetry(getRetries(RpcUtils.getMethodName(request.getRequest())));
            policy.setType(ClusterInvoker.TYPE_FAILOVER);
            policy.setRetryPolicy(retryPolicy);
        } else if (cluster instanceof FailfastClusterInvoker) {
            policy.setType(ClusterInvoker.TYPE_FAILFAST);
        } else if (cluster instanceof FailsafeClusterInvoker) {
            policy.setType(ClusterInvoker.TYPE_FAILSAFE);
        } else if (cluster instanceof FailbackClusterInvoker) {
            policy.setType(ClusterInvoker.TYPE_FAILBACK);
        } else if (cluster instanceof BroadcastClusterInvoker) {
            policy.setType(ClusterInvoker.TYPE_BROADCAST);
        } else if (cluster instanceof ForkingClusterInvoker) {
            policy.setType(ClusterInvoker.TYPE_FORKING);
        } else {
            policy.setType(ClusterInvoker.TYPE_FAILFAST);
        }
        return policy;
    }

    @SuppressWarnings("unchecked")
    public List<DubboEndpoint<?>> route(DubboOutboundRequest request) throws RpcException {
        List<Invoker<?>> invokers = cluster.getDirectory().list(request.getRequest());
        return invokers == null ? new ArrayList<>() : invokers.stream().map(DubboEndpoint::of).collect(Collectors.toList());
    }

    @Override
    public DubboOutboundResponse invoke(DubboOutboundRequest request, DubboEndpoint<?> endpoint) throws RpcException {
        Result result = endpoint.getInvoker().invoke(request.getRequest());
        return result.hasException() ? new DubboOutboundResponse(result, result.getException(), this::isRetryable)
                : new DubboOutboundResponse(result);
    }

    @Override
    public DubboOutboundResponse createResponse(Throwable throwable, DubboOutboundRequest request, DubboEndpoint<?> endpoint) {
        if (throwable == null) {
            return new DubboOutboundResponse(AsyncRpcResult.newDefaultAsyncResult(null, null, request.getRequest()));
        } else if (throwable instanceof RpcException) {
            return new DubboOutboundResponse(throwable, this::isRetryable);
        } else if (throwable instanceof LiveException) {
            return new DubboOutboundResponse(
                    new RpcException(RpcException.UNKNOWN_EXCEPTION, getError(throwable, request, endpoint)), this::isRetryable);
        }
        return new DubboOutboundResponse(
                new RpcException(RpcException.UNKNOWN_EXCEPTION,
                        getError(throwable, request, endpoint),
                        throwable.getCause() != null ? throwable.getCause() : throwable),
                this::isRetryable);
    }

    @Override
    public boolean isRetryable(Response response) {
        if (response.getResponse() == null) {
            return true;
        } else if (!(response.getThrowable() instanceof RpcException)) {
            return false;
        } else {
            RpcException exception = (RpcException) response.getThrowable();
            return exception.isNetwork() || exception.isTimeout();
        }
    }

    @Override
    public RpcException createNoProviderException(DubboOutboundRequest request) {
        Invocation invocation = request.getRequest();
        return new RpcException(RpcException.NO_INVOKER_AVAILABLE_AFTER_FILTER, "Failed to invoke the method "
                + invocation.getMethodName() + " in the service " + cluster.getInterface().getName()
                + ". No provider available for the service " + cluster.getDirectory().getConsumerUrl().getServiceKey()
                + " from registry " + cluster.getDirectory().getUrl().getAddress()
                + " on the consumer " + NetUtils.getLocalHost()
                + " using the dubbo version " + Version.getVersion()
                + ". Please check if the providers have been started and registered.");
    }

    @Override
    public RpcException createRejectException(RejectException exception) {
        return new RpcException(RpcException.FORBIDDEN_EXCEPTION, exception.getMessage());
    }

    @Override
    public RpcException createRetryExhaustedException(RetryExhaustedException exception, OutboundInvocation<DubboOutboundRequest> invocation) {
        String methodName = RpcUtils.getMethodName(invocation.getRequest().getRequest());
        Throwable cause = exception.getCause();
        RpcException le = cause instanceof RpcException ? (RpcException) cause : null;
        DubboOutboundRequest request = invocation.getRequest();
        Set<String> providers = request.getAttempts() == null ? new HashSet<>() : request.getAttempts();
        List<? extends Endpoint> instances = invocation.getInstances();
        return new RpcException(le != null ? le.getCode() : 0, "Failed to invoke the method "
                + methodName + " in the service " + cluster.getInterface().getName()
                + ". Tried " + exception.getAttempts() + " times of the providers " + providers
                + " (" + providers.size() + "/" + (instances == null ? 0 : instances.size())
                + ") from the registry " + cluster.getDirectory().getUrl().getAddress()
                + " on the consumer " + NetUtils.getLocalHost() + " using the dubbo version "
                + Version.getVersion() + ". Last error is: "
                + (le != null ? le.getMessage() : ""), le != null && le.getCause() != null ? le.getCause() : le);
    }

    /**
     * Retrieves the configured number of retries for a specific method invocation.
     *
     * @param methodName The name of the method for which the retry count is to be retrieved.
     * @return The number of retries configured for the specified method, defaulting to 1 if the
     * configured value is less than or equal to 0.
     */
    private int getRetries(String methodName) {
        int len = cluster.getUrl().getMethodParameter(methodName, RETRIES_KEY, DEFAULT_RETRIES) + 1;
        RpcContext context = RpcContext.getClientAttachment();
        Object retry = context.getObjectAttachment(RETRIES_KEY);
        if (retry instanceof Number) {
            len = ((Number) retry).intValue() + 1;
            context.removeAttachment(RETRIES_KEY);
        }
        if (len <= 0) {
            len = 1;
        }
        return len;
    }

    /**
     * Constructs a detailed error message for a given throwable and RPC call context.
     *
     * @param throwable The {@code Throwable} that represents the error encountered.
     * @param request   The {@code DubboOutboundRequest} that contains details about the RPC request.
     * @param endpoint  The {@code DubboEndpoint} that contains details about the endpoint being called.
     * @return A {@code String} representing the detailed error message.
     */
    private String getError(Throwable throwable, DubboOutboundRequest request, DubboEndpoint<?> endpoint) {
        if (endpoint == null) {
            return throwable.getMessage();
        }
        Invocation invocation = request.getRequest();
        return "Failed to call " + invocation.getServiceName() + "." + invocation.getMethodName()
                + " on remote server: " + endpoint.getInvoker().getUrl().getAddress() + ", cause by: "
                + throwable.getClass().getName() + ", message is: " + throwable.getMessage();
    }

}

