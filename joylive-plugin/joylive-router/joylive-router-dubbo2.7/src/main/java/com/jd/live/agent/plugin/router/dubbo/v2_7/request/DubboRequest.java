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
package com.jd.live.agent.plugin.router.dubbo.v2_7.request;

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

import java.util.function.Function;

import static com.jd.live.agent.governance.util.Predicates.isDubboSystemService;
import static com.jd.live.agent.plugin.router.dubbo.v2_7.exception.Dubbo27InboundThrower.THROWER;
import static org.apache.dubbo.common.constants.RegistryConstants.*;

/**
 * Defines a common interface for Dubbo RPC requests.
 * This interface serves as a marker for request types within the Dubbo framework, facilitating
 * the identification and processing of Dubbo-specific request data in RPC operations.
 */
public interface DubboRequest {

    /**
     * Gets the actual method name from invocation, handling generic calls.
     *
     * @param invocation the invocation
     * @return the method name
     */
    static String getMethodName(Invocation invocation) {
        return ("$invoke".equals(invocation.getMethodName()) || "$invokeAsync".equals(invocation.getMethodName()))
                && invocation.getArguments() != null
                && invocation.getArguments().length > 0
                && invocation.getArguments()[0] instanceof String
                ? (String) invocation.getArguments()[0]
                : invocation.getMethodName();
    }

    /**
     * Gets the actual arguments from invocation, handling generic calls.
     *
     * @param invocation the invocation
     * @return the arguments array
     */
    static Object[] getArguments(Invocation invocation) {
        return ("$invoke".equals(invocation.getMethodName()) || "$invokeAsync".equals(invocation.getMethodName()))
                && invocation.getArguments() != null
                && invocation.getArguments().length > 2
                && invocation.getArguments()[2] instanceof Object[]
                ? (Object[]) invocation.getArguments()[2]
                : invocation.getArguments();
    }

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
            this(request, null);
        }

        public DubboInboundRequest(Invocation request, Invoker<?> invoker) {
            super(request);
            invoker = invoker == null ? request.getInvoker() : invoker;
            URL url = invoker.getUrl();
            this.interfaceName = url.getServiceInterface();
            boolean requestMode = SERVICE_REGISTRY_TYPE.equals(request.getAttachment(REGISTRY_TYPE_KEY));
            String registryType = url.getParameter(REGISTRY_TYPE_KEY);
            boolean serviceMode = SERVICE_REGISTRY_TYPE.equals(registryType) || "all".equals(registryType);
            if (requestMode && serviceMode) {
                this.service = url.getParameter(CommonConstants.APPLICATION_KEY);
                this.path = interfaceName;
            } else {
                this.service = interfaceName;
                this.path = null;
            }

            this.group = url.getParameter(CommonConstants.GROUP_KEY);
            // fix RpcUtils issue
            this.method = DubboRequest.getMethodName(request);
            // fix RpcUtils issue
            this.arguments = DubboRequest.getArguments(request);
            this.attachments = request.getAttachments();
        }

        @Override
        public String getClientIp() {
            return RpcContext.getContext().getRemoteHost();
        }

        @Override
        public boolean isSystem() {
            return isDubboSystemService(interfaceName);
        }

        /**
         * Converts throwable to RPC result.
         *
         * @param e The throwable to convert
         * @return AsyncRpcResult containing the exception
         */
        public Result recover(Throwable e) {
            return AsyncRpcResult.newDefaultAsyncResult(THROWER.createException(e, this), this.request);
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
            this.interfaceName = url.getServiceInterface();
            String providedBy = url.getParameter(PROVIDED_BY);
            this.service = providedBy == null ? interfaceName : providedBy;
            this.group = url.getParameter(CommonConstants.GROUP_KEY);
            this.path = providedBy == null ? null : interfaceName;
            this.method = DubboRequest.getMethodName(request);
            this.arguments = DubboRequest.getArguments(request);
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
        public Function<Throwable, ErrorName> getErrorFunction() {
            return DUBBO_ERROR_FUNCTION;
        }

        @Override
        public boolean isSystem() {
            return isDubboSystemService(interfaceName);
        }

        @Override
        public RpcReturnType getReturnType() throws Exception {
            return DubboReturnType.of(request);
        }
    }
}
