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
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.transport.ClientTransport;
import com.jd.live.agent.governance.request.StickyRequest;

import java.util.ArrayList;
import java.util.List;

import static com.alipay.sofa.rpc.common.RpcConstants.INTERNAL_KEY_CLIENT_ROUTER_TIME_NANO;

/**
 * Represents a live cluster that manages sticky sessions for RPC requests.
 * This class is responsible for maintaining a consistent request routing strategy
 * to a specific service provider within the cluster based on a "sticky" identifier.
 * The sticky behavior ensures that requests are repeatedly sent to the same provider
 * for stateful interactions or session consistency.
 */
public class LiveCluster implements StickyRequest {

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
    public LiveCluster(AbstractCluster cluster) {
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

    /**
     * Routes the given request through the cluster's router chain to determine the
     * list of provider information (ProviderInfo) instances that can handle the request.
     *
     * @param request the request to route
     * @return a list of ProviderInfo objects that can potentially handle the request
     */
    public List<ProviderInfo> route(SofaRequest request) {
        long routerStartTime = System.nanoTime();
        List<ProviderInfo> result = cluster.getRouterChain().route(request, null);
        RpcInvokeContext.getContext().put(INTERNAL_KEY_CLIENT_ROUTER_TIME_NANO, System.nanoTime() - routerStartTime);

        result = result == null ? new ArrayList<>() : result;
        if (result.isEmpty()) {
            ProviderInfo directProvider = getDirectProvider();
            if (directProvider != null) {
                result.add(directProvider);
            }
        }
        return result;
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
     *         {@link RpcInternalContext}, or {@code null} if the IP address is not specified, is empty,
     *         or if the conversion to {@link ProviderInfo} fails for any reason.
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
    public boolean isConnected(ProviderInfo providerInfo) {
        ClientTransport lastTransport = cluster.connectionHolder.getAvailableClientTransport(providerInfo);
        return lastTransport != null && lastTransport.isAvailable();
    }

    /**
     * Invokes an RPC call on the specified provider with the given request.
     * This method applies the cluster's filter chain to the request before invoking the call.
     *
     * @param providerInfo the provider to invoke the call on
     * @param request      the request to be sent
     * @return the response from the provider
     * @throws SofaRpcException if an error occurs during the invocation
     */
    public SofaResponse invoke(ProviderInfo providerInfo, SofaRequest request) throws SofaRpcException {
        return cluster.filterChain(providerInfo, request);
    }
}

