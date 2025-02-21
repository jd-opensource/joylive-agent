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
package com.jd.live.agent.plugin.router.gprc.exception;

import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.*;
import com.jd.live.agent.governance.invoke.exception.AbstractInboundThrower;
import com.jd.live.agent.plugin.router.gprc.request.GrpcRequest.GrpcInboundRequest;
import io.grpc.InternalStatus;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;

/**
 * A concrete implementation of the InboundThrower interface for GRPC.
 *
 * @see GrpcInboundThrower
 */
public class GrpcInboundThrower extends AbstractInboundThrower<GrpcInboundRequest> {

    public static final GrpcInboundThrower THROWER = new GrpcInboundThrower();

    @Override
    protected StatusRuntimeException createUnReadyException(RejectUnreadyException exception, GrpcInboundRequest request) {
        return InternalStatus.asRuntimeException(GrpcStatus.createUnReadyException(exception), new Metadata(), false);
    }

    @Override
    protected StatusRuntimeException createLiveException(LiveException exception, GrpcInboundRequest request) {
        return InternalStatus.asRuntimeException(GrpcStatus.createLiveException(exception), new Metadata(), false);
    }

    @Override
    protected StatusRuntimeException createPermissionException(RejectPermissionException exception, GrpcInboundRequest request) {
        return InternalStatus.asRuntimeException(GrpcStatus.createPermissionException(exception), new Metadata(), false);
    }

    @Override
    protected StatusRuntimeException createAuthException(RejectAuthException exception, GrpcInboundRequest request) {
        return InternalStatus.asRuntimeException(GrpcStatus.createAuthException(exception), new Metadata(), false);
    }

    @Override
    protected StatusRuntimeException createLimitException(RejectLimitException exception, GrpcInboundRequest request) {
        return InternalStatus.asRuntimeException(GrpcStatus.createLimitException(exception), new Metadata(), false);
    }

    @Override
    protected StatusRuntimeException createCircuitBreakException(RejectCircuitBreakException exception, GrpcInboundRequest request) {
        return InternalStatus.asRuntimeException(GrpcStatus.createCircuitBreakException(exception), new Metadata(), false);
    }

    @Override
    protected StatusRuntimeException createEscapeException(RejectEscapeException exception, GrpcInboundRequest request) {
        return InternalStatus.asRuntimeException(GrpcStatus.createEscapeException(exception), new Metadata(), false);
    }

    @Override
    protected StatusRuntimeException createRejectException(RejectException exception, GrpcInboundRequest request) {
        return InternalStatus.asRuntimeException(GrpcStatus.createRejectException(exception), new Metadata(), false);
    }
}
