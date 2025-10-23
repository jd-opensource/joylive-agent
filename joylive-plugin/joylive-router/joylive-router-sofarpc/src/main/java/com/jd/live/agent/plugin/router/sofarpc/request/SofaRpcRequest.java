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

import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.jd.live.agent.governance.exception.ErrorName;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcInboundRequest;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcOutboundRequest;

import java.util.function.Function;

import static com.jd.live.agent.plugin.router.sofarpc.exception.SofaRpcInboundThrower.THROWER;

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

        @Override
        public String getClientIp() {
            return RpcInternalContext.getContext().getRemoteAddress().getAddress().getHostAddress();
        }

        /**
         * Creates error response from throwable.
         *
         * @param e The throwable to convert
         * @return SofaResponse with wrapped exception
         */
        public SofaResponse recover(Throwable e) {
            SofaResponse response = new SofaResponse();
            // app response can be exception
            response.setAppResponse(THROWER.createException(e, this));
            return response;
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

        private static final Function<Throwable, ErrorName> SOFARPC_ERROR_FUNCTION = throwable -> {
            if (throwable instanceof SofaRpcException) {
                return new ErrorName(null, String.valueOf(((SofaRpcException) throwable).getErrorType()));
            } else if (throwable instanceof SofaRpcRuntimeException) {
                return null;
            }
            return DEFAULT_ERROR_FUNCTION.apply(throwable);
        };

        private final SofaReturnType returnType;

        /**
         * Creates a new SofaRpcOutboundRequest with the specified SOFA request details and an optional sticky session
         * identifier. This constructor supports scenarios where sticky session routing is desired, allowing subsequent
         * requests to be routed to the same provider.
         *
         * @param request  The SOFA request containing the RPC call details.
         */
        public SofaRpcOutboundRequest(SofaRequest request) {
            super(request);
            this.returnType = SofaReturnType.of(request);
            String uniqueName = request.getTargetServiceUniqueName();
            int pos = uniqueName.indexOf(':');
            this.group = pos < 0 ? null : uniqueName.substring(pos + 1);
            this.service = pos < 0 ? uniqueName : uniqueName.substring(0, pos);
            this.method = !returnType.isGeneric() ? request.getMethodName() : (String) request.getMethodArgs()[0];
            this.arguments = !returnType.isGeneric() ? request.getMethodArgs() : (Object[]) request.getMethodArgs()[2];
            this.attachments = request.getRequestProps();
        }

        @Override
        public void setHeader(String key, String value) {
            if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
                request.getRequestProps().put(key, value);
            }
        }

        @Override
        public Function<Throwable, ErrorName> getErrorFunction() {
            return SOFARPC_ERROR_FUNCTION;
        }

        @Override
        public SofaReturnType getReturnType() {
            return returnType;
        }

    }
}
