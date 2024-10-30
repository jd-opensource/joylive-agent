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

import com.alipay.sofa.rpc.api.GenericContext;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.jd.live.agent.governance.exception.ErrorName;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcInboundRequest;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcOutboundRequest;
import com.jd.live.agent.governance.request.StickyRequest;

import java.util.function.Function;

import static com.jd.live.agent.plugin.router.sofarpc.exception.SofaRpcInboundThrower.THROWER;

/**
 * SofaRpcRequest
 *
 * @since 1.0.0
 */
public interface SofaRpcRequest {

    /**
     * generic call
     */
    String METHOD_$INVOKE = "$invoke";

    String METHOD_$GENERIC_INVOKE = "$genericInvoke";

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
         * Converts an object to a SofaRpc Result.
         * <p>
         * This method checks if the object is already a SofaRpc Result, and if so, returns it directly.
         * If the object is a Throwable, it creates a new SofaResponse with the Throwable wrapped in a DubboException.
         * Otherwise, it creates a new SofaResponse with the object as the result.
         * </p>
         *
         * @param obj the object to convert to a SofaRpc Result.
         * @return a SofaRpc Result representing the object.
         */
        public SofaResponse convert(Object obj) {
            SofaResponse response;
            if (obj instanceof SofaResponse) {
                response = (SofaResponse) obj;
            } else if (obj instanceof Throwable) {
                response = new SofaResponse();
                response.setAppResponse(THROWER.createException((Throwable) obj, this));
            } else {
                response = new SofaResponse();
                response.setAppResponse(obj);
            }
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

        private final StickyRequest stickyRequest;

        private final GenericType genericType;

        /**
         * Creates a new SofaRpcOutboundRequest without a sticky session identifier. This constructor is used
         * when sticky session routing is not required for the RPC call.
         * <p>
         * Initializes the request with the provided SOFA request details, extracting necessary information
         * such as service interface name, method name, arguments, and any attachments.
         * </p>
         *
         * @param request The SOFA request containing the RPC call details.
         */
        public SofaRpcOutboundRequest(SofaRequest request) {
            this(request, null);
        }

        /**
         * Creates a new SofaRpcOutboundRequest with the specified SOFA request details and an optional sticky session
         * identifier. This constructor supports scenarios where sticky session routing is desired, allowing subsequent
         * requests to be routed to the same provider.
         *
         * @param request  The SOFA request containing the RPC call details.
         * @param stickyRequest A supplier providing the sticky session identifier, or {@code null} if sticky routing is not used.
         */
        public SofaRpcOutboundRequest(SofaRequest request, StickyRequest stickyRequest) {
            super(request);
            this.stickyRequest = stickyRequest;
            this.genericType = computeGenericType();
            String uniqueName = request.getTargetServiceUniqueName();
            int pos = uniqueName.indexOf(':');
            this.group = pos < 0 ? null : uniqueName.substring(pos + 1);
            this.service = pos < 0 ? uniqueName : uniqueName.substring(0, pos);
            this.method = genericType == null ? request.getMethodName() : (String) request.getMethodArgs()[0];
            this.arguments = genericType == null ? request.getMethodArgs() : (Object[]) request.getMethodArgs()[2];
            this.attachments = request.getRequestProps();
        }

        @Override
        public String getStickyId() {
            return stickyRequest == null ? null : stickyRequest.getStickyId();
        }

        @Override
        public void setStickyId(String stickyId) {
            if (stickyRequest != null) {
                stickyRequest.setStickyId(stickyId);
            }
        }

        @Override
        public Function<Throwable, ErrorName> getErrorFunction() {
            return SOFARPC_ERROR_FUNCTION;
        }

        @Override
        public boolean isGeneric() {
            return genericType != null;
        }

        public GenericType getGenericType() {
            return genericType;
        }

        /**
         * Computes the generic type based on the request method name and arguments.
         *
         * @return The computed generic type, or null if no generic type could be determined.
         */
        private GenericType computeGenericType() {
            String methodName = request.getMethodName();
            if (METHOD_$INVOKE.equals(methodName)) {
                return new GenericType(RemotingConstants.SERIALIZE_FACTORY_NORMAL, null);
            } else if (METHOD_$GENERIC_INVOKE.equals(methodName)) {
                Object[] args = request.getMethodArgs();
                if (args.length == 3) {
                    return new GenericType(RemotingConstants.SERIALIZE_FACTORY_GENERIC, null);
                } else if (args.length == 4) {
                    if (args[3] instanceof GenericContext) {
                        return new GenericType(RemotingConstants.SERIALIZE_FACTORY_GENERIC, null);
                    }
                    if (args[3] instanceof Class) {
                        return new GenericType(RemotingConstants.SERIALIZE_FACTORY_MIX, (Class<?>) args[3]);
                    }
                } else if (args.length == 5) {
                    return new GenericType(RemotingConstants.SERIALIZE_FACTORY_MIX, (Class<?>) args[3]);
                }
            }
            return null;
        }
    }

    /**
     * Represents a generic type with a specified type and return type.
     */
    class GenericType {

        private final String type;

        private final Class<?> returnType;

        public GenericType(String type, Class<?> returnType) {
            this.type = type;
            this.returnType = returnType;
        }

        public String getType() {
            return type;
        }

        public Class<?> getReturnType() {
            return returnType;
        }
    }
}
