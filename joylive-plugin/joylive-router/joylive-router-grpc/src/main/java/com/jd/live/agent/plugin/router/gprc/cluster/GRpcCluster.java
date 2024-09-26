package com.jd.live.agent.plugin.router.gprc.cluster;

import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.governance.exception.RetryException;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.LiveCluster;
import com.jd.live.agent.plugin.router.gprc.instance.GRpcEndpoint;
import java.util.List;
import java.util.concurrent.CompletionStage;
import com.jd.live.agent.plugin.router.gprc.response.GRpcResponse.GRpcOutboundResponse;
import com.jd.live.agent.plugin.router.gprc.request.GRpcRequest.GRpcOutboundRequest;


public class GRpcCluster implements LiveCluster<GRpcOutboundRequest, GRpcOutboundResponse, GRpcEndpoint, RuntimeException> {

    @Override
    public CompletionStage<List<GRpcEndpoint>> route(GRpcOutboundRequest request) {
        System.out.println("---->route");
        return null;
    }

    @Override
    public CompletionStage<GRpcOutboundResponse> invoke(GRpcOutboundRequest request, GRpcEndpoint endpoint) {
        System.out.println("---->invoke");
        return null;
    }

    @Override
    public GRpcOutboundResponse createResponse(Throwable throwable, GRpcOutboundRequest request, GRpcEndpoint endpoint) {
        System.out.println("---->createResponse");
        return null;
    }

    @Override
    public RuntimeException createException(Throwable throwable, GRpcOutboundRequest request, GRpcEndpoint endpoint) {
        System.out.println("---->createException");
        return null;
    }

    @Override
    public RuntimeException createNoProviderException(GRpcOutboundRequest request) {
        System.out.println("---->createNoProviderException");
        return null;
    }

    @Override
    public RuntimeException createLimitException(RejectException exception, GRpcOutboundRequest request) {
        System.out.println("---->createLimitException");
        return null;
    }

    @Override
    public RuntimeException createCircuitBreakException(RejectException exception, GRpcOutboundRequest request) {
        System.out.println("---->createCircuitBreakException");
        return null;
    }

    @Override
    public RuntimeException createRejectException(RejectException exception, GRpcOutboundRequest request) {
        System.out.println("---->createRejectException");
        return null;
    }

    @Override
    public RuntimeException createRetryExhaustedException(RetryException.RetryExhaustedException exception, OutboundInvocation<GRpcOutboundRequest> invocation) {
        System.out.println("---->createRetryExhaustedException");
        return null;
    }
}
