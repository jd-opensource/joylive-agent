package com.jd.live.agent.plugin.router.gprc.cluster;

import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.LiveCluster;
import com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint;
import com.jd.live.agent.plugin.router.gprc.request.GrpcRequest.GrpcOutboundRequest;
import com.jd.live.agent.plugin.router.gprc.response.GrpcResponse.GrpcOutboundResponse;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CompletionStage;


public class GrpcCluster implements LiveCluster<GrpcOutboundRequest, GrpcOutboundResponse, GrpcEndpoint> {

    public GrpcCluster(SocketAddress socketAddress) {
    }

    @Override
    public CompletionStage<List<GrpcEndpoint>> route(GrpcOutboundRequest request) {
        return null;
    }

    @Override
    public CompletionStage<GrpcOutboundResponse> invoke(GrpcOutboundRequest request, GrpcEndpoint endpoint) {
        return null;
    }

    @Override
    public GrpcOutboundResponse createResponse(Throwable throwable, GrpcOutboundRequest request, GrpcEndpoint endpoint) {
        return null;
    }

    @Override
    public Throwable createException(Throwable throwable, GrpcOutboundRequest request) {
        return null;
    }

    @Override
    public Throwable createException(Throwable throwable, GrpcOutboundRequest request, GrpcEndpoint endpoint) {
        return null;
    }

    @Override
    public Throwable createException(Throwable throwable, OutboundInvocation<GrpcOutboundRequest> invocation) {
        return null;
    }
}
