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

import com.jd.live.agent.governance.exception.ErrorName;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcInboundRequest;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcOutboundRequest;
import com.jd.live.agent.governance.request.RpcReturnType;
import com.jd.live.agent.governance.request.StickySession;
import com.jd.live.agent.governance.request.StickySessionFactory;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.service.GenericException;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.function.Function;

import static com.jd.live.agent.governance.util.Predicates.isDubboSystemService;
import static com.jd.live.agent.plugin.router.dubbo.v3.exception.Dubbo3InboundThrower.THROWER;
import static org.apache.dubbo.common.constants.RegistryConstants.*;

/**
 * Defines a common interface for Dubbo RPC requests.
 * This interface serves as a marker for request types within the Dubbo framework, facilitating
 * the identification and processing of Dubbo-specific request data in RPC operations.
 */
public interface DubboRequest {

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
            return isDubboSystemService(interfaceName);
        }

        /**
         * Converts an object to a Dubbo Result.
         * <p>
         * This method checks if the object is already a Dubbo Result, and if so, returns it directly.
         * If the object is a Throwable, it creates a new AppResponse with the Throwable wrapped in a RpcException.
         * Otherwise, it creates a new AppResponse with the object as the result.
         * </p>
         *
         * @param obj the object to convert to a Dubbo Result.
         * @return a Dubbo Result representing the object.
         */
        public Result convert(Object obj) {
            if (obj instanceof Result) {
                return (Result) obj;
            } else if (obj instanceof Throwable) {
                return AsyncRpcResult.newDefaultAsyncResult(THROWER.createException((Throwable) obj, this), this.request);
            } else {
                return AsyncRpcResult.newDefaultAsyncResult(obj, this.request);
            }
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

        private final StickySessionFactory sessionFactory;

        private final URL url;

        public DubboOutboundRequest(Invocation request, StickySessionFactory sessionFactory) {
            super(request);
            this.sessionFactory = sessionFactory;
            this.url = request.getInvoker().getUrl();
            String providedBy = url.getParameter(PROVIDED_BY);
            this.interfaceName = url.getServiceInterface();
            this.service = providedBy == null ? interfaceName : providedBy;
            this.group = url.getParameter(CommonConstants.GROUP_KEY);
            this.path = providedBy == null ? null : interfaceName;
            this.method = RpcUtils.getMethodName(request);
            this.arguments = RpcUtils.getArguments(request);
            this.attachments = request.getAttachments();
        }

        public URL getUrl() {
            return url;
        }

        @Override
        public StickySession getStickySession(StickySessionFactory sessionFactory) {
            StickySession session = sessionFactory == null ? null : sessionFactory.getStickySession(this);
            return session == null && this.sessionFactory != null ? this.sessionFactory.getStickySession(this) : session;
        }

        @Override
        public void setHeader(String key, String value) {
            if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
                request.setAttachment(key, value);
            }
        }

        @Override
        public boolean isSystem() {
            return isDubboSystemService(interfaceName);
        }

        @Override
        public Function<Throwable, ErrorName> getErrorFunction() {
            return DUBBO_ERROR_FUNCTION;
        }

        @Override
        public RpcReturnType getReturnType() throws Exception {
            return DubboReturnType.of(request);
        }
    }
}
