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
package com.alipay.sofa.rpc.client;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRouteException;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.transport.ClientTransport;
import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.governance.exception.RetryException.RetryExhaustedException;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.invoke.cluster.LiveCluster;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.response.Response;
import com.jd.live.agent.plugin.router.sofarpc.instance.SofaRpcEndpoint;
import com.jd.live.agent.plugin.router.sofarpc.request.SofaRpcRequest.SofaRpcOutboundRequest;
import com.jd.live.agent.plugin.router.sofarpc.response.SofaRpcResponse.SofaRpcOutboundResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.alipay.sofa.rpc.common.RpcConstants.INTERNAL_KEY_CLIENT_ROUTER_TIME_NANO;
import static com.alipay.sofa.rpc.core.exception.RpcErrorType.CLIENT_ROUTER;

/**
 * Represents a live cluster specifically designed for managing Sofa RPC communications.
 * <p>
 * This class implements the {@link LiveCluster} interface, providing concrete implementations
 * for handling outbound requests and responses within a Sofa RPC environment. It encapsulates
 * the functionality necessary for routing, error handling, and maintaining session stickiness
 * across the cluster. The {@code SofaRpcCluster} ensures that RPC interactions are efficiently
 * managed, leveraging the underlying abstract cluster mechanisms while introducing RPC-specific
 * optimizations and configurations.
 * </p>
 *
 * @see LiveCluster
 */
public class SofaRpcCluster implements LiveCluster<SofaRpcOutboundRequest, SofaRpcOutboundResponse, SofaRpcEndpoint, SofaRpcException> {

    /**
     * The underlying abstract cluster that this live cluster is part of.
     */
    private final AbstractCluster cluster;

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
    public SofaRpcCluster(AbstractCluster cluster) {
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
    public ClusterPolicy getDefaultPolicy(SofaRpcOutboundRequest request) {
        ClusterPolicy policy = new ClusterPolicy();
        if (cluster instanceof FailoverCluster) {
            // no interval config in com.alipay.sofa.rpc.client.FailoverCluster
            RetryPolicy retryPolicy = new RetryPolicy();
            retryPolicy.setRetry(getRetries(request.getRequest().getMethodName()));
            policy.setType(ClusterInvoker.TYPE_FAILOVER);
            policy.setRetryPolicy(retryPolicy);
        } else {
            policy.setType(ClusterInvoker.TYPE_FAILFAST);
        }
        return policy;
    }

    @Override
    public CompletionStage<List<SofaRpcEndpoint>> route(SofaRpcOutboundRequest request) {
        long routerStartTime = System.nanoTime();
        List<ProviderInfo> providers = cluster.getRouterChain().route(request.getRequest(), null);
        RpcInvokeContext.getContext().put(INTERNAL_KEY_CLIENT_ROUTER_TIME_NANO, System.nanoTime() - routerStartTime);

        providers = providers == null ? new ArrayList<>() : providers;
        if (providers.isEmpty()) {
            ProviderInfo directProvider = getDirectProvider();
            if (directProvider != null) {
                providers.add(directProvider);
            }
        }
        return CompletableFuture.completedFuture(providers.stream()
                .map(e -> new SofaRpcEndpoint(e, this::isConnected))
                .collect(Collectors.toList()));
    }

    @Override
    public CompletionStage<SofaRpcOutboundResponse> invoke(SofaRpcOutboundRequest request, SofaRpcEndpoint endpoint) {
        try {
            SofaResponse response = cluster.filterChain(endpoint.getProvider(), request.getRequest());
            return CompletableFuture.completedFuture(new SofaRpcOutboundResponse(response));
        } catch (SofaRpcException e) {
            return CompletableFuture.completedFuture(new SofaRpcOutboundResponse(e, this::isRetryable));
        }
    }

    @Override
    public SofaRpcOutboundResponse createResponse(Throwable throwable, SofaRpcOutboundRequest request, SofaRpcEndpoint endpoint) {
        if (throwable == null) {
            return new SofaRpcOutboundResponse(new SofaResponse());
        }
        return new SofaRpcOutboundResponse(createException(throwable, request, endpoint), this::isRetryable);
    }

    @Override
    public boolean isRetryable(Response response) {
        if (response.getResponse() == null) {
            return true;
        } else if (!(response.getThrowable() instanceof SofaRpcException)) {
            return false;
        }
        SofaRpcException exception = (SofaRpcException) response.getThrowable();
        switch (exception.getErrorType()) {
            case RpcErrorType.SERVER_BUSY:
            case RpcErrorType.CLIENT_TIMEOUT:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isDestroyed() {
        return cluster.destroyed;
    }

    @Override
    public SofaRpcException createUnReadyException(SofaRpcOutboundRequest request) {
        return createUnReadyException("Rpc cluster invoker for " + cluster.getConsumerConfig().getInterfaceId()
                + " on consumer " + Ipv4.getLocalHost()
                + " is now destroyed! Can not invoke any more.", request);
    }

    @Override
    public SofaRpcException createUnReadyException(String message, SofaRpcOutboundRequest request) {
        return new SofaRpcException(RpcErrorType.UNKNOWN, message);
    }

    @Override
    public SofaRpcException createException(Throwable throwable, SofaRpcOutboundRequest request, SofaRpcEndpoint endpoint) {
        if (throwable == null) {
            return null;
        } else if (throwable instanceof SofaRpcException) {
            return (SofaRpcException) throwable;
        } else {
            String message = getError(throwable, request, endpoint);
            if (throwable instanceof LiveException) {
                return new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, message);
            }
            Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
            return new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, message, cause);
        }
    }

    @Override
    public SofaRpcException createNoProviderException(SofaRpcOutboundRequest request) {
        return new SofaRouteException(
                LogCodes.getLog(LogCodes.ERROR_NO_AVAILABLE_PROVIDER,
                        request.getRequest().getTargetServiceUniqueName(), "[]"));
    }

    @Override
    public SofaRpcException createRejectException(RejectException exception, SofaRpcOutboundRequest request) {
        return new SofaRpcException(CLIENT_ROUTER, exception.getMessage());
    }

    @Override
    public SofaRpcException createRetryExhaustedException(RetryExhaustedException exception, OutboundInvocation<SofaRpcOutboundRequest> invocation) {
        SofaRpcOutboundRequest request = invocation.getRequest();
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        return cause instanceof SofaRpcException ? (SofaRpcException) cause :
                new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR,
                        "Failed to call " + request.getRequest().getInterfaceName()
                                + "." + request.getRequest().getMethodName()
                                + " , cause by unknown exception: " + cause.getClass().getName()
                                + ", message is: " + cause.getMessage());
    }

    @Override
    public void onRetry(int retries) {
        if (RpcInternalContext.isAttachmentEnable()) {
            RpcInternalContext.getContext().setAttachment(RpcConstants.INTERNAL_KEY_INVOKE_TIMES, retries + 1);
        }
    }

    /**
     * Attempts to retrieve a direct {@link ProviderInfo} instance based on a target IP address specified
     * in the {@link RpcInternalContext}. This method is particularly useful for scenarios requiring direct
     * routing to a specific service provider, bypassing the usual load balancing mechanisms.
     *
     * <p>The target IP address is expected to be provided as an attachment in the {@link RpcInternalContext}
     * under the key defined by {@link RpcConstants#HIDDEN_KEY_PINPOINT}. If the target IP is specified and
     * is not an empty string, the method attempts to convert it into a {@link ProviderInfo} object using
     * {@link ProviderHelper#toProviderInfo(String)}. If the conversion is successful, the resulting
     * {@link ProviderInfo} object is returned. Otherwise, or if no target IP is specified, the method
     * returns {@code null}.</p>
     *
     * <p>This method is particularly useful in testing or debugging scenarios where direct communication
     * with a specific service provider instance is necessary. It also serves use cases where a particular
     * service instance needs to be targeted due to its unique characteristics or state.</p>
     *
     * @return The {@link ProviderInfo} corresponding to the direct target IP address specified in the
     * {@link RpcInternalContext}, or {@code null} if the IP address is not specified, is empty,
     * or if the conversion to {@link ProviderInfo} fails for any reason.
     */
    private ProviderInfo getDirectProvider() {
        String targetIP = (String) RpcInternalContext.peekContext().getAttachment(RpcConstants.HIDDEN_KEY_PINPOINT);
        if (targetIP != null && !targetIP.isEmpty()) {
            try {
                return ProviderHelper.toProviderInfo(targetIP);
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    /**
     * Checks whether there is an active connection to the specified provider within the cluster.
     *
     * @param providerInfo the provider to check the connection for
     * @return {@code true} if there is an active connection to the provider; {@code false} otherwise
     */
    private boolean isConnected(ProviderInfo providerInfo) {
        ClientTransport lastTransport = cluster.connectionHolder.getAvailableClientTransport(providerInfo);
        return lastTransport != null && lastTransport.isAvailable();
    }

    /**
     * Retrieves the configured number of retries for a specific method invocation.
     *
     * @param methodName The name of the method for which the retry count is to be retrieved.
     * @return The number of retries configured for the specified method
     */
    private int getRetries(String methodName) {
        return cluster.consumerConfig.getMethodRetries(methodName);
    }

    /**
     * Constructs a detailed error message for a given throwable and RPC call context.
     *
     * @param throwable The {@code Throwable} that represents the error encountered.
     * @param request   The {@code SofaRpcOutboundRequest} that contains details about the RPC request.
     * @param endpoint  The {@code SofaRpcEndpoint} that contains details about the endpoint being called.
     * @return A {@code String} representing the detailed error message.
     */
    private String getError(Throwable throwable, SofaRpcOutboundRequest request, SofaRpcEndpoint endpoint) {
        if (endpoint == null) {
            return throwable.getMessage();
        }
        SofaRequest sofaRequest = request.getRequest();
        return "Failed to call " + sofaRequest.getInterfaceName() + "." + sofaRequest.getMethodName()
                + " on remote server: " + endpoint.getProvider() + ", cause by: "
                + throwable.getClass().getName() + ", message is: " + throwable.getMessage();
    }
}

