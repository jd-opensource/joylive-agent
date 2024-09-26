package com.jd.live.agent.plugin.router.gprc.cluster;

import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.governance.exception.RetryException;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.LiveCluster;
import com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint;
import java.util.List;
import java.util.concurrent.CompletionStage;
import com.jd.live.agent.plugin.router.gprc.response.GrpcResponse.GrpcOutboundResponse;
import com.jd.live.agent.plugin.router.gprc.request.GrpcRequest.GrpcOutboundRequest;


public class GrpcCluster implements LiveCluster<GrpcOutboundRequest, GrpcOutboundResponse, GrpcEndpoint, RuntimeException> {

    @Override
    public CompletionStage<List<GrpcEndpoint>> route(GrpcOutboundRequest request) {
        System.out.println("---->route");
        return null;
    }

    @Override
    public CompletionStage<GrpcOutboundResponse> invoke(GrpcOutboundRequest request, GrpcEndpoint endpoint) {
        System.out.println("---->invoke");
        return null;
    }

    @Override
    public GrpcOutboundResponse createResponse(Throwable throwable, GrpcOutboundRequest request, GrpcEndpoint endpoint) {
        System.out.println("---->createResponse");
        return null;
    }

    @Override
    public RuntimeException createException(Throwable throwable, GrpcOutboundRequest request, GrpcEndpoint endpoint) {
        System.out.println("---->createException");
        return null;
    }

    @Override
    public RuntimeException createNoProviderException(GrpcOutboundRequest request) {
        System.out.println("---->createNoProviderException");
        return null;
    }

    @Override
    public RuntimeException createLimitException(RejectException exception, GrpcOutboundRequest request) {
        System.out.println("---->createLimitException");
        return null;
    }

    @Override
    public RuntimeException createCircuitBreakException(RejectException exception, GrpcOutboundRequest request) {
        System.out.println("---->createCircuitBreakException");
        return null;
    }

    @Override
    public RuntimeException createRejectException(RejectException exception, GrpcOutboundRequest request) {
        System.out.println("---->createRejectException");
        return null;
    }

    @Override
    public RuntimeException createRetryExhaustedException(RetryException.RetryExhaustedException exception, OutboundInvocation<GrpcOutboundRequest> invocation) {
        System.out.println("---->createRetryExhaustedException");
        return null;
    }
}
