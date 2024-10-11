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
package com.alibaba.dubbo.rpc.cluster.support;

import com.alibaba.dubbo.rpc.RpcException;
import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.*;
import com.jd.live.agent.governance.invoke.exception.AbstractInboundThrower;
import com.jd.live.agent.plugin.router.dubbo.v2_6.request.DubboRequest.DubboInboundRequest;

/**
 * A concrete implementation of the InboundThrower interface for Dubbo 2.6.x.
 *
 * @see AbstractInboundThrower
 */
public class Dubbo26InboundThrower extends AbstractInboundThrower<DubboInboundRequest, RpcException> {

    public static final Dubbo26InboundThrower THROWER = new Dubbo26InboundThrower();

    @Override
    protected boolean isNativeException(Throwable throwable) {
        return throwable instanceof RpcException;
    }

    @Override
    protected RpcException createUnReadyException(RejectUnreadyException exception, DubboInboundRequest request) {
        return new RpcException(exception.getMessage());
    }

    @Override
    protected RpcException createUnknownException(Throwable throwable, DubboInboundRequest request) {
        String message = throwable.getMessage();
        if (throwable instanceof LiveException) {
            return new RpcException(RpcException.UNKNOWN_EXCEPTION, message);
        }
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        return new RpcException(RpcException.UNKNOWN_EXCEPTION, message, cause);
    }

    @Override
    protected RpcException createPermissionException(RejectPermissionException exception, DubboInboundRequest request) {
        return new RpcException(RpcException.FORBIDDEN_EXCEPTION, exception.getMessage());
    }

    @Override
    protected RpcException createAuthException(RejectAuthException exception, DubboInboundRequest request) {
        return new RpcException(RpcException.FORBIDDEN_EXCEPTION, exception.getMessage());
    }

    @Override
    protected RpcException createLimitException(RejectLimitException exception, DubboInboundRequest request) {
        return new RpcException(RpcException.FORBIDDEN_EXCEPTION, exception.getMessage());
    }

    @Override
    protected RpcException createCircuitBreakException(RejectCircuitBreakException exception, DubboInboundRequest request) {
        return new RpcException(RpcException.FORBIDDEN_EXCEPTION, exception.getMessage());
    }

    @Override
    protected RpcException createEscapeException(RejectEscapeException exception, DubboInboundRequest request) {
        return new RpcException(RpcException.FORBIDDEN_EXCEPTION, exception.getMessage());
    }

    @Override
    protected RpcException createRejectException(RejectException exception, DubboInboundRequest request) {
        return new RpcException(RpcException.FORBIDDEN_EXCEPTION, exception.getMessage());
    }
}
