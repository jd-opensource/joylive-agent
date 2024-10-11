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
package com.jd.live.agent.plugin.router.sofarpc.cluster;

import com.alipay.sofa.rpc.client.AbstractCluster;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRouteException;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.log.LogCodes;
import com.jd.live.agent.bootstrap.exception.FaultException;
import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectNoProviderException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectUnreadyException;
import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.governance.exception.RetryException.RetryExhaustedException;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.exception.AbstractOutboundThrower;
import com.jd.live.agent.plugin.router.sofarpc.instance.SofaRpcEndpoint;
import com.jd.live.agent.plugin.router.sofarpc.request.SofaRpcRequest.SofaRpcOutboundRequest;

/**
 * A concrete implementation of the OutboundThrower interface for Dubbo 2.7.x.
 *
 * @see AbstractOutboundThrower
 */
public class SofaRpcOutboundThrower extends AbstractOutboundThrower<SofaRpcOutboundRequest, SofaRpcEndpoint, SofaRpcException> {

    private final AbstractCluster cluster;

    public SofaRpcOutboundThrower(AbstractCluster cluster) {
        this.cluster = cluster;
    }

    @Override
    protected boolean isNativeException(Throwable throwable) {
        return throwable instanceof SofaRpcException;
    }

    @Override
    protected SofaRpcException createUnReadyException(RejectUnreadyException exception, SofaRpcOutboundRequest request) {
        String message = exception.getMessage() == null
                ? "Rpc cluster invoker for " + cluster.getConsumerConfig().getInterfaceId()
                + " on consumer " + Ipv4.getLocalHost()
                + " is now destroyed! Can not invoke any more."
                : exception.getMessage();
        return new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, message);
    }

    @Override
    protected SofaRpcException createUnknownException(Throwable throwable, SofaRpcOutboundRequest request, SofaRpcEndpoint endpoint) {
        String message = getError(throwable, request, endpoint);
        if (throwable instanceof LiveException) {
            return new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, message);
        }
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        return new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, message, cause);
    }

    @Override
    protected SofaRpcException createFaultException(FaultException exception, SofaRpcOutboundRequest request) {
        Integer code = exception.getCode();
        code = code == null ? RpcErrorType.CLIENT_UNDECLARED_ERROR : code;
        return new SofaRpcException(code, exception.getMessage());
    }

    @Override
    protected SofaRpcException createCircuitBreakException(RejectCircuitBreakException exception, SofaRpcOutboundRequest request) {
        return new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, exception.getMessage());
    }

    @Override
    protected SofaRpcException createNoProviderException(RejectNoProviderException exception, SofaRpcOutboundRequest request) {
        return new SofaRouteException(
                LogCodes.getLog(LogCodes.ERROR_NO_AVAILABLE_PROVIDER,
                        request.getRequest().getTargetServiceUniqueName(), "[]"));
    }

    @Override
    protected SofaRpcException createRejectException(RejectException exception, SofaRpcOutboundRequest request) {
        return new SofaRpcException(RpcErrorType.CLIENT_ROUTER, exception.getMessage());
    }

    @Override
    protected SofaRpcException createRetryExhaustedException(RetryExhaustedException exception, OutboundInvocation<SofaRpcOutboundRequest> invocation) {
        SofaRpcOutboundRequest request = invocation.getRequest();
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        return cause instanceof SofaRpcException ? (SofaRpcException) cause :
                new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR,
                        "Failed to call " + request.getRequest().getInterfaceName()
                                + "." + request.getRequest().getMethodName()
                                + " , cause by unknown exception: " + cause.getClass().getName()
                                + ", message is: " + cause.getMessage());
    }

    @Override
    protected String getError(Throwable throwable, SofaRpcOutboundRequest request, SofaRpcEndpoint endpoint) {
        if (endpoint == null) {
            return throwable.getMessage();
        }
        SofaRequest sofaRequest = request.getRequest();
        return "Failed to call " + sofaRequest.getInterfaceName() + "." + sofaRequest.getMethodName()
                + " on remote server: " + endpoint.getProvider() + ", cause by: "
                + throwable.getClass().getName() + ", message is: " + throwable.getMessage();
    }
}
