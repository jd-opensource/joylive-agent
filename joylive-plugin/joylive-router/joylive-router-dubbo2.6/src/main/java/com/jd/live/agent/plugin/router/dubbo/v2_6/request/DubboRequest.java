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
package com.jd.live.agent.plugin.router.dubbo.v2_6.request;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.service.GenericException;
import com.alibaba.dubbo.rpc.support.RpcUtils;
import com.jd.live.agent.governance.exception.ErrorName;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcInboundRequest;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcOutboundRequest;
import com.jd.live.agent.governance.request.RpcReturnType;
import com.jd.live.agent.governance.request.StickySession;
import com.jd.live.agent.governance.request.StickySessionFactory;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.jd.live.agent.plugin.router.dubbo.v2_6.exception.Dubbo26InboundThrower.THROWER;

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
     * relevant information from the Dubbo {@link Invocation} object, such as service interface,
     * group, method name, arguments, and attachments.
     * </p>
     *
     * @see AbstractRpcInboundRequest
     */
    class DubboInboundRequest extends AbstractRpcInboundRequest<Invocation> implements DubboRequest {

        private final Predicate<String> systemPredicate;

        public DubboInboundRequest(Invocation request, Predicate<String> systemPredicate) {
            super(request);
            this.systemPredicate = systemPredicate;
            URL url = request.getInvoker().getUrl();
            this.service = url.getServiceInterface();
            this.group = url.getParameter(Constants.GROUP_KEY);
            this.method = RpcUtils.getMethodName(request);
            this.arguments = RpcUtils.getArguments(request);
            this.attachments = request.getAttachments();
        }

        @Override
        public String getClientIp() {
            return RpcContext.getContext().getRemoteHost();
        }

        @Override
        public boolean isSystem() {
            return systemPredicate.test(service);
        }

        /**
         * Converts throwable to RPC result.
         *
         * @param e The throwable to convert
         * @return AsyncRpcResult containing the exception
         */
        public Result recover(Throwable e) {
            return new RpcResult(THROWER.createException(e, this));
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

        private final StickySessionFactory sessionFactory;

        private final URL url;

        private final Predicate<String> systemPredicate;

        public DubboOutboundRequest(Invocation request, StickySessionFactory sessionFactory, Predicate<String> systemPredicate) {
            super(request);
            this.sessionFactory = sessionFactory;
            this.systemPredicate = systemPredicate;
            this.url = request.getInvoker().getUrl();
            this.service = url.getServiceInterface();
            this.group = url.getParameter(Constants.GROUP_KEY);
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
                if (request instanceof RpcInvocation) {
                    ((RpcInvocation) request).setAttachment(key, value);
                } else {
                    Map<String, String> map = request.getAttachments();
                    if (map != null) {
                        map.put(key, value);
                    }
                }
            }
        }

        @Override
        public Function<Throwable, ErrorName> getErrorFunction() {
            return DUBBO_ERROR_FUNCTION;
        }

        @Override
        public boolean isSystem() {
            return systemPredicate.test(service);
        }

        @Override
        public RpcReturnType getReturnType() throws Exception {
            return DubboReturnType.of(request);
        }
    }
}
