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
package com.jd.live.agent.plugin.router.dubbo.v3.request;

import com.alibaba.dubbo.rpc.support.RpcUtils;
import com.jd.live.agent.governance.exception.ErrorName;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcInboundRequest;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcOutboundRequest;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.service.GenericException;
import org.apache.dubbo.rpc.service.GenericService;

import java.util.function.Function;

import static org.apache.dubbo.common.constants.RegistryConstants.*;

/**
 * Defines a common interface for Dubbo RPC requests.
 * This interface serves as a marker for request types within the Dubbo framework, facilitating
 * the identification and processing of Dubbo-specific request data in RPC operations.
 */
public interface DubboRequest {

    String METADATA_SERVICE = "org.apache.dubbo.metadata.MetadataService";

    /**
     * generic call
     */
    String METHOD_$INVOKE = "$invoke";

    String METHOD_$INVOKE_ASYNC = "$invokeAsync";

    /**
     * Represents an inbound request in a Dubbo RPC communication.
     * <p>
     * This class extends {@link AbstractRpcInboundRequest} to provide a concrete implementation
     * tailored for Dubbo's protocol and data handling requirements. It extracts and stores
     * relevant information from the Dubbo {@link com.alibaba.dubbo.rpc.Invocation} object, such as service interface,
     * group, method name, arguments, and attachments.
     * </p>
     *
     * @see AbstractRpcInboundRequest
     */
    class DubboInboundRequest extends AbstractRpcInboundRequest<Invocation> implements DubboRequest {

        private final String interfaceName;

        public DubboInboundRequest(Invocation request) {
            super(request);
            URL url = request.getInvoker().getUrl();
            this.interfaceName = url.getServiceInterface();
            boolean requestMode = SERVICE_REGISTRY_TYPE.equals(request.getAttachment(REGISTRY_TYPE_KEY));
            String registryType = url.getParameter(REGISTRY_TYPE_KEY, DEFAULT_REGISTER_MODE_ALL);
            boolean serviceMode = SERVICE_REGISTRY_TYPE.equals(registryType) || DEFAULT_REGISTER_MODE_ALL.equals(registryType);
            if (requestMode && serviceMode) {
                this.service = url.getApplication();
                this.path = interfaceName;
            } else {
                this.service = interfaceName;
                this.path = null;
            }

            this.group = url.getParameter(CommonConstants.GROUP_KEY);
            this.method = RpcUtils.getMethodName(request);
            this.arguments = RpcUtils.getArguments(request);
            this.attachments = request.getAttachments();
        }

        @Override
        public String getClientIp() {
            return RpcContext.getServiceContext().getRemoteHost();
        }

        @Override
        public boolean isSystem() {
            return METADATA_SERVICE.equals(interfaceName);
        }
    }

    /**
     * Represents an outbound request in a Dubbo RPC communication.
     * <p>
     * Similar to {@link DubboInboundRequest}, this class extends {@link AbstractRpcOutboundRequest}
     * to cater to the specific needs of Dubbo's communication protocol for outbound requests. It
     * encapsulates the necessary information for dispatching an RPC request, including the target
     * service interface, group, method name, arguments, and attachments.
     * </p>
     *
     * @see AbstractRpcOutboundRequest
     */
    class DubboOutboundRequest extends AbstractRpcOutboundRequest<Invocation> implements DubboRequest {

        private static final Function<Throwable, ErrorName> DUBBO_ERROR_FUNCTION = throwable -> {
            if (throwable instanceof RpcException) {
                return new ErrorName(null, String.valueOf(((RpcException) throwable).getCode()));
            } else if (throwable instanceof GenericException) {
                return new ErrorName(((GenericException) throwable).getExceptionClass(), null);
            }
            return DEFAULT_ERROR_FUNCTION.apply(throwable);
        };

        private final String interfaceName;

        public DubboOutboundRequest(Invocation request) {
            super(request);
            URL url = request.getInvoker().getUrl();
            String providedBy = url.getParameter(PROVIDED_BY);
            this.interfaceName = url.getServiceInterface();
            this.service = providedBy == null ? interfaceName : providedBy;
            this.group = url.getParameter(CommonConstants.GROUP_KEY);
            this.path = providedBy == null ? null : interfaceName;
            this.method = RpcUtils.getMethodName(request);
            this.arguments = RpcUtils.getArguments(request);
            this.attachments = request.getAttachments();
        }

        @Override
        public boolean isSystem() {
            return METADATA_SERVICE.equals(interfaceName);
        }

        @Override
        public Function<Throwable, ErrorName> getErrorFunction() {
            return DUBBO_ERROR_FUNCTION;
        }

        @Override
        public boolean isGeneric() {
            Invoker<?> invoker = request.getInvoker();
            String methodName = request.getMethodName();
            return ((
                    METHOD_$INVOKE.equals(methodName)
                            || METHOD_$INVOKE_ASYNC.equals(methodName))
                    && invoker.getInterface().isAssignableFrom(GenericService.class));
        }
    }
}
