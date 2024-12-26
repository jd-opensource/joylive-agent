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

import com.jd.live.agent.bootstrap.exception.FaultException;
import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectNoProviderException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectUnreadyException;
import com.jd.live.agent.governance.exception.RetryException.RetryExhaustedException;
import com.jd.live.agent.governance.exception.RetryException.RetryTimeoutException;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.exception.AbstractOutboundThrower;
import com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint;
import com.jd.live.agent.plugin.router.gprc.request.GrpcRequest.GrpcOutboundRequest;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;

/**
 * A concrete implementation of the OutboundThrower interface for Grpc.
 *
 * @see AbstractOutboundThrower
 */
public class GrpcOutboundThrower extends AbstractOutboundThrower<GrpcOutboundRequest, GrpcEndpoint> {

    public static final GrpcOutboundThrower THROWER = new GrpcOutboundThrower();

    @Override
    public Throwable createException(Throwable throwable, GrpcOutboundRequest request, GrpcEndpoint endpoint) {
        if (throwable instanceof GrpcException) {
            return throwable.getCause();
        }
        return super.createException(throwable, request, endpoint);
    }

    @Override
    protected StatusRuntimeException createUnReadyException(RejectUnreadyException exception, GrpcOutboundRequest request) {
        return GrpcStatus.createUnReadyException(exception).asRuntimeException(new Metadata());
    }

    @Override
    protected StatusRuntimeException createLiveException(LiveException exception, GrpcOutboundRequest request, GrpcEndpoint endpoint) {
        return GrpcStatus.createLiveException(exception).asRuntimeException(new Metadata());
    }

    @Override
    protected StatusRuntimeException createFaultException(FaultException exception, GrpcOutboundRequest request) {
        return GrpcStatus.createFaultException(exception).asRuntimeException(new Metadata());
    }

    @Override
    protected StatusRuntimeException createCircuitBreakException(RejectCircuitBreakException exception, GrpcOutboundRequest request) {
        return GrpcStatus.createCircuitBreakException(exception).asRuntimeException(new Metadata());
    }

    @Override
    protected StatusRuntimeException createNoProviderException(RejectNoProviderException exception, GrpcOutboundRequest request) {
        return GrpcStatus.createNoProviderException(exception).asRuntimeException(new Metadata());
    }

    @Override
    protected StatusRuntimeException createRejectException(RejectException exception, GrpcOutboundRequest request) {
        return GrpcStatus.createRejectException(exception).asRuntimeException(new Metadata());
    }

    @Override
    protected StatusRuntimeException createRetryExhaustedException(RetryExhaustedException exception, OutboundInvocation<GrpcOutboundRequest> invocation) {
        return GrpcStatus.createRetryExhaustedException(exception).asRuntimeException(new Metadata());
    }

    @Override
    protected StatusRuntimeException createRetryTimeoutException(RetryTimeoutException exception, OutboundInvocation<GrpcOutboundRequest> invocation) {
        return GrpcStatus.createRetryTimeoutException(exception).asRuntimeException(new Metadata());
    }

}
