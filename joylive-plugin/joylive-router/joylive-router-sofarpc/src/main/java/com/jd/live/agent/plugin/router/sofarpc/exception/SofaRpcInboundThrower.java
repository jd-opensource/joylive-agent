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
package com.jd.live.agent.plugin.router.sofarpc.exception;

import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.*;
import com.jd.live.agent.governance.invoke.exception.AbstractInboundThrower;
import com.jd.live.agent.plugin.router.sofarpc.request.SofaRpcRequest.SofaRpcInboundRequest;

/**
 * A concrete implementation of the InboundThrower interface for Dubbo 2.7.x.
 *
 * @see AbstractInboundThrower
 */
public class SofaRpcInboundThrower extends AbstractInboundThrower<SofaRpcInboundRequest> {

    public static final SofaRpcInboundThrower THROWER = new SofaRpcInboundThrower();

    @Override
    protected SofaRpcException createUnReadyException(RejectUnreadyException exception, SofaRpcInboundRequest request) {
        // Retry to another instance by consumer.
        return new SofaRpcException(RpcErrorType.SERVER_BUSY, exception.getMessage());
    }

    @Override
    protected SofaRpcException createLiveException(LiveException exception, SofaRpcInboundRequest request) {
        // Retry to another instance by consumer.
        return new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, exception.getMessage());
    }

    @Override
    protected SofaRpcException createPermissionException(RejectPermissionException exception, SofaRpcInboundRequest request) {
        // Only SERVER_BUSY and CLIENT_TIMEOUT will be retried by consumer.
        // see {@link com.alipay.sofa.rpc.client.FailoverCluster}
        return new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, exception.getMessage());
    }

    @Override
    protected SofaRpcException createAuthException(RejectAuthException exception, SofaRpcInboundRequest request) {
        // Only SERVER_BUSY and CLIENT_TIMEOUT will be retried by consumer.
        // see {@link com.alipay.sofa.rpc.client.FailoverCluster}
        return new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, exception.getMessage());
    }

    @Override
    protected SofaRpcException createLimitException(RejectLimitException exception, SofaRpcInboundRequest request) {
        // Retry to another instance by consumer.
        return new SofaRpcException(RpcErrorType.SERVER_BUSY, exception.getMessage());
    }

    @Override
    protected SofaRpcException createCircuitBreakException(RejectCircuitBreakException exception, SofaRpcInboundRequest request) {
        // Retry to another instance by consumer.
        return new SofaRpcException(RpcErrorType.SERVER_BUSY, exception.getMessage());
    }

    @Override
    protected SofaRpcException createEscapeException(RejectEscapeException exception, SofaRpcInboundRequest request) {
        // Retry to another instance by consumer.
        return new SofaRpcException(RpcErrorType.SERVER_BUSY, exception.getMessage());
    }

    @Override
    protected SofaRpcException createRejectException(RejectException exception, SofaRpcInboundRequest request) {
        // Only SERVER_BUSY and CLIENT_TIMEOUT will be retried by consumer.
        // see {@link com.alipay.sofa.rpc.client.FailoverCluster}
        return new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, exception.getMessage());
    }
}
