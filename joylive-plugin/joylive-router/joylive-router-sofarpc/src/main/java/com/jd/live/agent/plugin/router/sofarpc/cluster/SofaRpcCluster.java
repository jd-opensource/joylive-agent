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
package com.jd.live.agent.plugin.router.sofarpc.cluster;

import com.alipay.sofa.rpc.client.*;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRouteException;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.transport.ClientTransport;
import com.jd.live.agent.bootstrap.exception.FaultException;
import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.*;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.core.util.type.ClassDesc;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.governance.exception.RetryException.RetryExhaustedException;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.invoke.cluster.LiveCluster;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.response.ServiceError;
import com.jd.live.agent.plugin.router.sofarpc.instance.SofaRpcEndpoint;
import com.jd.live.agent.plugin.router.sofarpc.request.SofaRpcRequest.SofaRpcOutboundRequest;
import com.jd.live.agent.plugin.router.sofarpc.response.SofaRpcResponse.SofaRpcOutboundResponse;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.alipay.sofa.rpc.common.RpcConstants.INTERNAL_KEY_CLIENT_ROUTER_TIME_NANO;
import static com.alipay.sofa.rpc.core.exception.RpcErrorType.CLIENT_ROUTER;
import static com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException.getCircuitBreakException;

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

    private static final Logger logger = LoggerFactory.getLogger(SofaRpcCluster.class);

    /**
     * The underlying abstract cluster that this live cluster is part of.
     */
    private final AbstractCluster cluster;

    private final ObjectParser parser;

    private final FieldDesc consumerConfigField;

    private final FieldDesc connectionHolderField;

    private final FieldDesc destroyedField;

    private final Method fieldChainMethod;

    /**
     * The identifier used for stickiness. This ID is used to route requests to
     * the same provider consistently.
     */
    private String stickyId;

    public SofaRpcCluster(AbstractCluster cluster, ObjectParser parser) {
        this.cluster = cluster;
        this.parser = parser;
        ClassDesc classDesc = ClassUtils.describe(cluster.getClass());
        this.consumerConfigField = classDesc.getFieldList().getField("consumerConfig");
        this.connectionHolderField = classDesc.getFieldList().getField("connectionHolder");
        this.destroyedField = classDesc.getFieldList().getField("destroyed");
        this.fieldChainMethod = classDesc.getMethodList().getMethods("filterChain").get(0);
        fieldChainMethod.setAccessible(true);
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
        List<SofaRpcEndpoint> endpoints = new ArrayList<>(providers.size());
        for (ProviderInfo provider : providers) {
            endpoints.add(new SofaRpcEndpoint(provider, this::isConnected));
        }
        return CompletableFuture.completedFuture(endpoints);
    }

    @Override
    public CompletionStage<SofaRpcOutboundResponse> invoke(SofaRpcOutboundRequest request, SofaRpcEndpoint endpoint) {
        try {
            SofaResponse response = (SofaResponse) fieldChainMethod.invoke(cluster, endpoint.getProvider(), request.getRequest());
            return CompletableFuture.completedFuture(new SofaRpcOutboundResponse(response));
        } catch (Throwable e) {
            return CompletableFuture.completedFuture(new SofaRpcOutboundResponse(new ServiceError(e, false), this::isRetryable));
        }
    }

    @Override
    public SofaRpcOutboundResponse createResponse(Throwable throwable, SofaRpcOutboundRequest request, SofaRpcEndpoint endpoint) {
        if (throwable == null) {
            return new SofaRpcOutboundResponse(new SofaResponse());
        }
        RejectCircuitBreakException circuitBreakException = getCircuitBreakException(throwable);
        if (circuitBreakException != null) {
            DegradeConfig config = circuitBreakException.getConfig();
            if (config != null) {
                try {
                    return new SofaRpcOutboundResponse(createResponse(request, config));
                } catch (Throwable e) {
                    logger.warn("Exception occurred when create degrade response from circuit break. caused by " + e.getMessage(), e);
                    return new SofaRpcOutboundResponse(new ServiceError(createException(throwable, request, endpoint), false), null);
                }
            }
        }
        return new SofaRpcOutboundResponse(new ServiceError(createException(throwable, request, endpoint), false), this::isRetryable);
    }

    @Override
    public boolean isRetryable(Throwable throwable) {
        if (!(throwable instanceof SofaRpcException)) {
            return false;
        }
        SofaRpcException exception = (SofaRpcException) throwable;
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
        return (Boolean) destroyedField.get(cluster);
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
        } else if (throwable instanceof RejectException) {
            return createRejectException((RejectException) throwable, request);
        } else if (throwable instanceof FaultException) {
            Integer code = ((FaultException) throwable).getCode();
            code = code == null ? RpcErrorType.CLIENT_UNDECLARED_ERROR : code;
            return new SofaRpcException(code, getError(throwable, request, endpoint));
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
    public SofaRpcException createPermissionException(RejectPermissionException exception, SofaRpcOutboundRequest request) {
        return new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, exception.getMessage());
    }

    @Override
    public SofaRpcException createAuthException(RejectAuthException exception, SofaRpcOutboundRequest request) {
        return new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, exception.getMessage());
    }

    @Override
    public SofaRpcException createLimitException(RejectLimitException exception, SofaRpcOutboundRequest request) {
        return new SofaRpcException(RpcErrorType.SERVER_BUSY, exception.getMessage());
    }

    @Override
    public SofaRpcException createCircuitBreakException(RejectCircuitBreakException exception, SofaRpcOutboundRequest request) {
        return new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, exception.getMessage());
    }

    @Override
    public SofaRpcException createNoProviderException(RejectNoProviderException exception, SofaRpcOutboundRequest request) {
        return new SofaRouteException(
                LogCodes.getLog(LogCodes.ERROR_NO_AVAILABLE_PROVIDER,
                        request.getRequest().getTargetServiceUniqueName(), "[]"));
    }

    @Override
    public SofaRpcException createEscapeException(RejectEscapeException exception, SofaRpcOutboundRequest request) {
        return new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, exception.getMessage());
    }

    @Override
    public SofaRpcException createRejectException(RejectException exception, SofaRpcOutboundRequest request) {
        if (exception instanceof RejectNoProviderException) {
            return createNoProviderException((RejectNoProviderException) exception, request);
        } else if (exception instanceof RejectAuthException) {
            return createAuthException((RejectAuthException) exception, request);
        } else if (exception instanceof RejectPermissionException) {
            return createPermissionException((RejectPermissionException) exception, request);
        } else if (exception instanceof RejectLimitException) {
            return createLimitException((RejectLimitException) exception, request);
        } else if (exception instanceof RejectCircuitBreakException) {
            return createCircuitBreakException((RejectCircuitBreakException) exception, request);
        }
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
        ConnectionHolder connectionHolder = (ConnectionHolder) connectionHolderField.get(cluster);
        ClientTransport lastTransport = connectionHolder.getAvailableClientTransport(providerInfo);
        return lastTransport != null && lastTransport.isAvailable();
    }

    /**
     * Retrieves the configured number of retries for a specific method invocation.
     *
     * @param methodName The name of the method for which the retry count is to be retrieved.
     * @return The number of retries configured for the specified method
     */
    private int getRetries(String methodName) {
        ConsumerConfig<?> config = (ConsumerConfig<?>) consumerConfigField.get(cluster);
        return config.getMethodRetries(methodName);
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

    /**
     * Creates a {@link SofaResponse} based on the provided {@link SofaRpcOutboundRequest} and {@link DegradeConfig}.
     * The response is configured with the status code, headers, and body specified in the degrade configuration.
     *
     * @param request       the original request containing headers.
     * @param degradeConfig the degrade configuration specifying the response details such as status code, headers, and body.
     * @return a {@link SofaResponse} configured according to the degrade configuration.
     */
    private SofaResponse createResponse(SofaRpcOutboundRequest request, DegradeConfig degradeConfig) {
        SofaRequest sofaRequest = request.getRequest();
        String body = degradeConfig.getResponseBody();
        SofaResponse result = new SofaResponse();
        if (degradeConfig.getAttributes() != null) {
            result.setResponseProps(new HashMap<>(degradeConfig.getAttributes()));
        }
        if (body != null) {
            // TODO generic & callback & async
            Method method = sofaRequest.getMethod();
            Type type = method.getGenericReturnType();
            Object value;
            if (void.class == type) {
                // happens when generic invoke or void return
                value = null;
            } else {
                value = parser.read(new StringReader(body), type);
            }
            result.setAppResponse(value);
        }

        return result;
    }
}

