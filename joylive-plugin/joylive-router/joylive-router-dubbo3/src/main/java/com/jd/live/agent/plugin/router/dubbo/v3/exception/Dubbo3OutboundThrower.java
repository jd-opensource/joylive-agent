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
package com.jd.live.agent.plugin.router.dubbo.v3.exception;

import com.jd.live.agent.bootstrap.exception.FaultException;
import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectNoProviderException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectUnreadyException;
import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.governance.exception.RetryException.RetryExhaustedException;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.exception.AbstractOutboundThrower;
import com.jd.live.agent.plugin.router.dubbo.v3.instance.DubboEndpoint;
import com.jd.live.agent.plugin.router.dubbo.v3.request.DubboRequest.DubboOutboundRequest;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A concrete implementation of the OutboundThrower interface for Dubbo 2.7.x.
 *
 * @see AbstractOutboundThrower
 */
public class Dubbo3OutboundThrower extends AbstractOutboundThrower<DubboOutboundRequest, DubboEndpoint<?>, RpcException> {

    private final AbstractClusterInvoker cluster;

    public Dubbo3OutboundThrower(AbstractClusterInvoker cluster) {
        this.cluster = cluster;
    }

    @Override
    protected boolean isNativeException(Throwable throwable) {
        return throwable instanceof RpcException;
    }

    @Override
    protected RpcException createUnReadyException(RejectUnreadyException exception, DubboOutboundRequest request) {
        String message = exception.getMessage() == null
                ? "Rpc cluster invoker for " + cluster.getInterface()
                + " on consumer " + Ipv4.getLocalHost()
                + " use dubbo version " + Version.getVersion()
                + " is now destroyed! Can not invoke any more."
                : exception.getMessage();
        return new RpcException(message);
    }

    @Override
    protected RpcException createUnknownException(Throwable throwable, DubboOutboundRequest request, DubboEndpoint<?> endpoint) {
        String message = getError(throwable, request, endpoint);
        if (throwable instanceof LiveException) {
            return new RpcException(RpcException.UNKNOWN_EXCEPTION, message);
        }
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        return new RpcException(RpcException.UNKNOWN_EXCEPTION, message, cause);
    }

    @Override
    protected RpcException createFaultException(FaultException exception, DubboOutboundRequest request) {
        Integer code = exception.getCode();
        code = code == null ? RpcException.UNKNOWN_EXCEPTION : code;
        return new RpcException(code, exception.getMessage());
    }

    @Override
    protected RpcException createCircuitBreakException(RejectCircuitBreakException exception, DubboOutboundRequest request) {
        return new RpcException(RpcException.FORBIDDEN_EXCEPTION, exception.getMessage());
    }

    @Override
    protected RpcException createNoProviderException(RejectNoProviderException exception, DubboOutboundRequest request) {
        Invocation invocation = request.getRequest();
        String message = "Failed to invoke the method "
                + invocation.getMethodName() + " in the service " + cluster.getInterface().getName()
                + ". No provider available for the service " + cluster.getUrl().getServiceKey()
                + " from registry " + cluster.getRegistryUrl().getAddress()
                + " on the consumer " + NetUtils.getLocalHost()
                + " using the dubbo version " + Version.getVersion()
                + ". Please check if the providers have been started and registered.";
        return new RpcException(message);
    }

    @Override
    protected RpcException createRejectException(RejectException exception, DubboOutboundRequest request) {
        return new RpcException(RpcException.FORBIDDEN_EXCEPTION, exception.getMessage());
    }

    @Override
    protected RpcException createRetryExhaustedException(RetryExhaustedException exception, OutboundInvocation<DubboOutboundRequest> invocation) {
        Throwable cause = exception.getCause();
        RpcException le = cause instanceof RpcException ? (RpcException) cause : null;
        String methodName = RpcUtils.getMethodName(invocation.getRequest().getRequest());
        DubboOutboundRequest request = invocation.getRequest();
        Set<String> providers = request.getAttempts() == null ? new HashSet<>() : request.getAttempts();
        List<? extends Endpoint> instances = invocation.getInstances();

        String message = "Failed to invoke the method "
                + methodName + " in the service " + cluster.getInterface().getName()
                + ". Tried " + exception.getAttempts() + " times of the providers " + providers
                + " (" + providers.size() + "/" + (instances == null ? 0 : instances.size())
                + ") from the registry " + cluster.getRegistryUrl().getAddress()
                + " on the consumer " + NetUtils.getLocalHost() + " using the dubbo version "
                + Version.getVersion() + ". Last error is: "
                + (le != null ? le.getMessage() : "");
        return new RpcException(
                le != null ? le.getCode() : RpcException.UNKNOWN_EXCEPTION,
                message,
                le != null && le.getCause() != null ? le.getCause() : le);
    }

    @Override
    protected String getError(Throwable throwable, DubboOutboundRequest request, DubboEndpoint<?> endpoint) {
        if (endpoint == null) {
            return throwable.getMessage();
        }
        Invocation invocation = request.getRequest();
        return "Failed to call " + invocation.getInvoker().getInterface().getName() + "." + invocation.getMethodName()
                + " on remote server: " + endpoint.getInvoker().getUrl().getAddress() + ", cause by: "
                + throwable.getClass().getName() + ", message is: " + throwable.getMessage();
    }
}
