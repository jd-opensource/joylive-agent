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
package com.jd.live.agent.plugin.router.sofarpc.request;

import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.SofaRouteException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.log.LogCodes;
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcInboundRequest;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcOutboundRequest;

/**
 * SofaRpcRequest
 *
 * @since 1.0.0
 */
public interface SofaRpcRequest {

    /**
     * Represents an inbound RPC request for the SOFA framework, encapsulating the necessary
     * details for processing the request on the server side.
     * <p>
     * This class extends {@link AbstractRpcInboundRequest} with a specific focus on SOFA RPC requests,
     * providing a structure to handle incoming service method invocations. It captures essential
     * details such as the service interface name, method name, arguments, and any additional
     * properties attached with the request. This information facilitates the execution of the
     * corresponding service method on the server.
     * </p>
     *
     * @see AbstractRpcInboundRequest for the base class functionality.
     */
    class SofaRpcInboundRequest extends AbstractRpcInboundRequest<SofaRequest> implements SofaRpcRequest {

        /**
         * Constructs a new {@code SofaRpcInboundRequest} with the specified SOFA request details.
         * <p>
         * Initializes the request with comprehensive details about the service method to be executed,
         * including the service interface name, the unique name of the target service, the method name,
         * method arguments, and any additional properties (attachments) that may accompany the request.
         * This constructor parses the unique service name to extract the service group if specified.
         * </p>
         *
         * @param request the {@link SofaRequest} containing the details of the service method invocation.
         */
        public SofaRpcInboundRequest(SofaRequest request) {
            super(request);
            this.service = request.getInterfaceName();
            String uniqueName = request.getTargetServiceUniqueName();
            int pos = uniqueName.lastIndexOf(':');
            this.group = pos < 0 ? null : uniqueName.substring(pos + 1);
            this.method = request.getMethodName();
            this.arguments = request.getMethodArgs();
            this.attachments = request.getRequestProps();
        }
    }

    /**
     * Represents an outbound RPC request specifically designed for the SOFA framework.
     * <p>
     * This class encapsulates the details required to execute a remote procedure call using the SOFA framework,
     * including service identification, method invocation details, and any additional attachments that may be necessary
     * for the call. It extends the generic {@link AbstractRpcOutboundRequest} class, providing SOFA-specific
     * implementation details.
     * </p>
     *
     * @see AbstractRpcOutboundRequest for more information on the base class functionality.
     */
    class SofaRpcOutboundRequest extends AbstractRpcOutboundRequest<SofaRequest> implements SofaRpcRequest {

        /**
         * Lazily initialized identifier used for sticky connections, typically based on the target service's IP and port.
         * <p>
         * This ID is determined by inspecting the current RPC context for a specific attachment that indicates the target
         * service's location. If present, this information is used to construct a unique identifier for the request, which
         * can be used to optimize connection reuse.
         * </p>
         */
        private final LazyObject<String> stickyId = new LazyObject<>(SofaRpcOutboundRequest::getStickyIdFromContext);

        /**
         * Constructs a new {@code SofaRpcOutboundRequest} with the specified SOFA request details.
         * <p>
         * This constructor initializes the request with comprehensive details about the service method to be invoked,
         * including the service interface name, method name, method arguments, and any additional properties attached
         * to the request.
         * </p>
         *
         * @param request the {@link SofaRequest} containing the details of the SOFA service method to be invoked.
         */
        public SofaRpcOutboundRequest(SofaRequest request) {
            super(request);
            this.service = request.getInterfaceName();
            String uniqueName = request.getTargetServiceUniqueName();
            int pos = uniqueName.lastIndexOf(':');
            this.group = pos < 0 ? null : uniqueName.substring(pos + 1);
            this.method = request.getMethodName();
            this.arguments = request.getMethodArgs();
            this.attachments = request.getRequestProps();
        }

        @Override
        public String getStickyId() {
            return stickyId.get();
        }

        @Override
        public RuntimeException createNoAvailableEndpointException() {
            return new SofaRouteException(LogCodes.getLog(LogCodes.ERROR_TARGET_URL_INVALID, request.getTargetServiceUniqueName(), "[]"));
        }

        /**
         * Attempts to extract a sticky ID from the current RPC context.
         * <p>
         * This method inspects the current {@link RpcInternalContext} for an attachment indicating the target
         * service's IP and port. If found, it constructs and returns a unique identifier based on this information.
         * </p>
         *
         * @return a unique identifier for the target service, or {@code null} if it cannot be determined.
         */
        private static String getStickyIdFromContext() {
            RpcInternalContext context = RpcInternalContext.peekContext();
            String targetIP = (String) context.getAttachment(RpcConstants.HIDDEN_KEY_PINPOINT);
            if (targetIP != null && !targetIP.isEmpty()) {
                try {
                    ProviderInfo providerInfo = ProviderHelper.toProviderInfo(targetIP);
                    return providerInfo.getHost() + ":" + providerInfo.getPort();
                } catch (Throwable ignore) {
                }
            }
            return null;
        }
    }
}
