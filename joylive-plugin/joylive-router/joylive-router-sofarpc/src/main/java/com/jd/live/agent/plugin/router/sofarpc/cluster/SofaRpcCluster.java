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

import com.alipay.hessian.generic.model.GenericObject;
import com.alipay.sofa.rpc.client.*;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.transport.ClientTransport;
import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.AbstractLiveCluster;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.invoke.cluster.LiveCluster;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.policy.service.loadbalance.StickyType;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.request.StickySession;
import com.jd.live.agent.governance.request.StickySession.DefaultStickySession;
import com.jd.live.agent.plugin.router.sofarpc.exception.SofaRpcOutboundThrower;
import com.jd.live.agent.plugin.router.sofarpc.instance.SofaRpcEndpoint;
import com.jd.live.agent.plugin.router.sofarpc.request.SofaRpcRequest.GenericType;
import com.jd.live.agent.plugin.router.sofarpc.request.SofaRpcRequest.SofaRpcOutboundRequest;
import com.jd.live.agent.plugin.router.sofarpc.response.SofaLiveCallback;
import com.jd.live.agent.plugin.router.sofarpc.response.SofaRpcResponse.SofaRpcOutboundResponse;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.alipay.sofa.rpc.common.RpcConstants.INTERNAL_KEY_CLIENT_ROUTER_TIME_NANO;
import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getQuietly;

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
public class SofaRpcCluster extends AbstractLiveCluster<SofaRpcOutboundRequest, SofaRpcOutboundResponse, SofaRpcEndpoint> {

    private static final ErrorPredicate RETRY_PREDICATE = new ErrorPredicate.DefaultErrorPredicate(
            throwable -> throwable instanceof SofaRpcException && (
                    (((SofaRpcException) throwable)).getErrorType() == RpcErrorType.SERVER_BUSY
                            || (((SofaRpcException) throwable)).getErrorType() == RpcErrorType.CLIENT_TIMEOUT), null);
    /**
     * The underlying abstract cluster that this live cluster is part of.
     */
    private final AbstractCluster cluster;

    private final ObjectParser parser;

    private final ConsumerConfig<?> config;

    private final ConnectionHolder connectionHolder;

    private final FieldAccessor destroyed;

    private final Method filterChainMethod;

    private final SofaRpcOutboundThrower thrower;

    private final StickySession stickySession;

    public SofaRpcCluster(AbstractCluster cluster, ObjectParser parser) {
        this.cluster = cluster;
        this.parser = parser;
        this.thrower = new SofaRpcOutboundThrower(cluster);
        this.config = getQuietly(cluster, "consumerConfig");
        this.stickySession = config.isSticky() ? new DefaultStickySession(StickyType.PREFERRED) : null;
        this.connectionHolder = getQuietly(cluster, "connectionHolder");
        this.destroyed = getAccessor(cluster.getClass(), "destroyed");
        this.filterChainMethod = ClassUtils.describe(cluster.getClass()).getMethodList().getMethods("filterChain").get(0);
        filterChainMethod.setAccessible(true);
    }

    @Override
    public StickySession getStickySession(ServiceRequest request) {
        return stickySession;
    }

    @Override
    public ClusterPolicy getDefaultPolicy(SofaRpcOutboundRequest request) {
        ClusterPolicy policy = new ClusterPolicy();
        if (cluster instanceof FailoverCluster) {
            // no interval config in com.alipay.sofa.rpc.client.FailoverCluster
            RetryPolicy retryPolicy = new RetryPolicy();
            retryPolicy.setRetry(config.getMethodRetries(request.getRequest().getMethodName()));
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
        SofaRequest sofaRequest = request.getRequest();
        CompletionStage<Object> future = getFuture(sofaRequest);
        try {
            SofaResponse response = (SofaResponse) filterChainMethod.invoke(cluster, endpoint.getProvider(), sofaRequest);
            return CompletableFuture.completedFuture(new SofaRpcOutboundResponse(response, future));
        } catch (Throwable e) {
            return CompletableFuture.completedFuture(new SofaRpcOutboundResponse(new ServiceError(e, false), getRetryPredicate()));
        } finally {
            SofaLiveCallback.FUTURE.remove();
        }
    }

    @Override
    public ErrorPredicate getRetryPredicate() {
        return RETRY_PREDICATE;
    }

    @Override
    public boolean isDestroyed() {
        return (Boolean) destroyed.get(cluster);
    }

    @Override
    public Throwable createException(Throwable throwable, SofaRpcOutboundRequest request) {
        return thrower.createException(throwable, request);
    }

    @Override
    public Throwable createException(Throwable throwable, SofaRpcOutboundRequest request, SofaRpcEndpoint endpoint) {
        return thrower.createException(throwable, request, endpoint);
    }

    @Override
    public Throwable createException(Throwable throwable, OutboundInvocation<SofaRpcOutboundRequest> invocation) {
        return thrower.createException(throwable, invocation);
    }

    @Override
    public void onRetry(SofaRpcOutboundRequest request, int retries) {
        if (RpcInternalContext.isAttachmentEnable()) {
            RpcInternalContext.getContext().setAttachment(RpcConstants.INTERNAL_KEY_INVOKE_TIMES, retries + 1);
        }
    }

    @Override
    protected SofaRpcOutboundResponse createResponse(SofaRpcOutboundRequest request) {
        return new SofaRpcOutboundResponse(new SofaResponse());
    }

    @Override
    protected SofaRpcOutboundResponse createResponse(SofaRpcOutboundRequest request, DegradeConfig degradeConfig) {
        SofaRequest sofaRequest = request.getRequest();
        String body = degradeConfig.getResponseBody();
        SofaResponse result = new SofaResponse();
        result.setResponseProps(new HashMap<>(degradeConfig.getAttributes()));
        if (body != null) {
            Object value;
            if (request.isGeneric()) {
                GenericType genericType = request.getGenericType();
                if (degradeConfig.isText()) {
                    value = body;
                } else if (RemotingConstants.SERIALIZE_FACTORY_GENERIC.equals(genericType.getType())) {
                    value = convertGenericObject(parser.read(new StringReader(body), Object.class));
                } else if (RemotingConstants.SERIALIZE_FACTORY_MIX.equals(genericType.getType())) {
                    value = parser.read(new StringReader(body), genericType.getReturnType());
                } else {
                    value = parser.read(new StringReader(body), request.loadClass(degradeConfig.getContentType(), Object.class));
                }
            } else {
                Method method = sofaRequest.getMethod();
                Type type = method.getGenericReturnType();
                if (void.class == type) {
                    // void return
                    value = null;
                } else {
                    value = parser.read(new StringReader(body), type);
                }
            }
            result.setAppResponse(value);
        }

        return new SofaRpcOutboundResponse(result);
    }

    @Override
    protected SofaRpcOutboundResponse createResponse(ServiceError error, ErrorPredicate predicate) {
        return new SofaRpcOutboundResponse(error, predicate);
    }

    /**
     * Registers asynchronous callback for SOFA RPC request.
     *
     * <p>Creates and attaches a {@link CompletionStage} to the request based on invocation type:
     * <ul>
     *   <li>For callback-type: wraps callback with {@link SofaLiveCallback}</li>
     *   <li>For future-type: binds new future to thread local.</li>
     * </ul>
     *
     * @param sofaRequest the RPC request to register
     * @return CompletionStage for async result, or null if not applicable
     */
    private CompletionStage<Object> getFuture(SofaRequest sofaRequest) {
        CompletableFuture<Object> future = null;
        String invokeType = sofaRequest.getInvokeType();
        if (RpcConstants.INVOKER_TYPE_CALLBACK.equals(invokeType)) {
            SofaResponseCallback<?> callback = sofaRequest.getSofaResponseCallback();
            callback = callback != null ? callback : cluster.getConsumerConfig().getMethodOnreturn(sofaRequest.getMethodName());
            future = new CompletableFuture<>();
            callback = new SofaLiveCallback(callback, future);
            sofaRequest.setSofaResponseCallback(callback);
        } else if (RpcConstants.INVOKER_TYPE_FUTURE.equals(invokeType)) {
            future = new CompletableFuture<>();
            SofaLiveCallback.FUTURE.set(future);
        }
        return future;
    }

    /**
     * Attempts to retrieve a direct {@link ProviderInfo} instance based on a target IP address specified
     * in the {@link RpcInternalContext}. This method is particularly useful for scenarios requiring direct
     * routing to a specific service provider, bypassing the usual load balancing mechanisms.
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
        ClientTransport lastTransport = connectionHolder.getAvailableClientTransport(providerInfo);
        return lastTransport != null && lastTransport.isAvailable();
    }

    private Object convertGenericObject(Object value) {
        if (value instanceof Map) {
            GenericObject object = new GenericObject(value.getClass().getName());
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                object.putField(entry.getKey().toString(), convertGenericObject(entry.getValue()));
            }
            return object;
        } else {
            return value;
        }
    }

}

