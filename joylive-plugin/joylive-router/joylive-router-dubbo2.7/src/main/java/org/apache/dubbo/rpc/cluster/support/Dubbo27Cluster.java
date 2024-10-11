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
package org.apache.dubbo.rpc.cluster.support;


import com.alibaba.dubbo.rpc.support.RpcUtils;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.AbstractLiveCluster;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.invoke.cluster.LiveCluster;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.response.ServiceError;
import com.jd.live.agent.plugin.router.dubbo.v2_7.instance.DubboEndpoint;
import com.jd.live.agent.plugin.router.dubbo.v2_7.request.DubboRequest.DubboOutboundRequest;
import com.jd.live.agent.plugin.router.dubbo.v2_7.response.DubboResponse.DubboOutboundResponse;
import org.apache.dubbo.rpc.*;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException.getCircuitBreakException;
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
public class Dubbo27Cluster extends AbstractLiveCluster<DubboOutboundRequest, DubboOutboundResponse, DubboEndpoint<?>, RpcException> {

    private static final Logger logger = LoggerFactory.getLogger(Dubbo27Cluster.class);

    private final AbstractClusterInvoker cluster;

    private final ObjectParser parser;

    private final Dubbo27OutboundThrower thrower;

    /**
     * The identifier used for stickiness. This ID is used to route requests to
     * the same provider consistently.
     */
    private String stickyId;

    public Dubbo27Cluster(AbstractClusterInvoker cluster, ObjectParser parser) {
        this.cluster = cluster;
        this.parser = parser;
        this.thrower = new Dubbo27OutboundThrower(cluster);
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
            // no retry interval in org.apache.dubbo.rpc.cluster.support.FailoverClusterInvoker
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
            List<Invoker<?>> invokers = cluster.getDirectory().list(request.getRequest());
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
            DubboOutboundResponse response = new DubboOutboundResponse(result, this::isRetryable);
            return CompletableFuture.completedFuture(response);
        } catch (Throwable e) {
            return CompletableFuture.completedFuture(new DubboOutboundResponse(new ServiceError(e, false), this::isRetryable));
        }
    }

    @Override
    public DubboOutboundResponse createResponse(Throwable throwable, DubboOutboundRequest request, DubboEndpoint<?> endpoint) {
        if (throwable == null) {
            return new DubboOutboundResponse(AsyncRpcResult.newDefaultAsyncResult(null, null, request.getRequest()));
        }
        RejectCircuitBreakException circuitBreakException = getCircuitBreakException(throwable);
        if (circuitBreakException != null) {
            DegradeConfig config = circuitBreakException.getConfig();
            if (config != null) {
                try {
                    return new DubboOutboundResponse(createResponse(request, config));
                } catch (Throwable e) {
                    logger.warn("Exception occurred when create degrade response from circuit break. caused by " + e.getMessage(), e);
                    return new DubboOutboundResponse(new ServiceError(createException(throwable, request, endpoint), false), null);
                }
            }
        }
        return new DubboOutboundResponse(new ServiceError(createException(throwable, request, endpoint), false), this::isRetryable);
    }

    @Override
    public boolean isRetryable(Throwable throwable) {
        if (!(throwable instanceof RpcException)) {
            return false;
        } else {
            RpcException exception = (RpcException) throwable;
            return exception.isNetwork() || exception.isTimeout();
        }
    }

    @Override
    public boolean isDestroyed() {
        return cluster.isDestroyed();
    }

    @Override
    public RpcException createException(Throwable throwable, DubboOutboundRequest request) {
        return thrower.createException(throwable, request);
    }

    @Override
    public RpcException createException(Throwable throwable, DubboOutboundRequest request, DubboEndpoint<?> endpoint) {
        return thrower.createException(throwable, request, endpoint);
    }

    @Override
    public RpcException createException(Throwable throwable, OutboundInvocation<DubboOutboundRequest> invocation) {
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
        int len = cluster.getUrl().getMethodParameter(methodName, RETRIES_KEY, DEFAULT_RETRIES) + 1;
        RpcContext rpcContext = RpcContext.getContext();
        Object retry = rpcContext.getObjectAttachment(RETRIES_KEY);
        if (retry instanceof Number) {
            len = ((Number) retry).intValue() + 1;
            rpcContext.removeAttachment(RETRIES_KEY);
        }
        if (len <= 0) {
            len = 1;
        }

        return len;
    }

    /**
     * Creates a {@link Result} based on the provided {@link DubboOutboundRequest} and {@link DegradeConfig}.
     * The response is configured with the status code, headers, and body specified in the degrade configuration.
     *
     * @param request       the original request containing headers.
     * @param degradeConfig the degrade configuration specifying the response details such as status code, headers, and body.
     * @return a {@link Result} configured according to the degrade configuration.
     */
    private Result createResponse(DubboOutboundRequest request, DegradeConfig degradeConfig) {
        RpcInvocation invocation = (RpcInvocation) request.getRequest();
        String body = degradeConfig.getResponseBody();
        AppResponse response = new AppResponse();
        response.setAttachments(degradeConfig.getAttributes());
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
            response.setValue(value);
        }

        return AsyncRpcResult.newDefaultAsyncResult(response, invocation);
    }

}

