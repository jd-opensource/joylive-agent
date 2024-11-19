package com.jd.live.agent.plugin.router.gprc.cluster;

import com.jd.live.agent.core.util.CollectionUtils;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.AbstractLiveCluster;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint;
import com.jd.live.agent.plugin.router.gprc.loadbalance.LiveDiscovery;
import com.jd.live.agent.plugin.router.gprc.request.GrpcRequest.GrpcOutboundRequest;
import com.jd.live.agent.plugin.router.gprc.response.GrpcResponse.GrpcOutboundResponse;
import io.grpc.ClientCall;
import io.grpc.LoadBalancer;
import io.grpc.Metadata;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


public class GrpcCluster extends AbstractLiveCluster<GrpcOutboundRequest, GrpcOutboundResponse, GrpcEndpoint> {

    private ClientCall<?, ?> clientCall;

    public GrpcCluster(ClientCall<?, ?> clientCall) {
        this.clientCall = clientCall;
    }

    @Override
    public CompletionStage<List<GrpcEndpoint>> route(GrpcOutboundRequest request) {
        List<LoadBalancer.Subchannel> subchannels = LiveDiscovery.getSubchannel(request.getService());
        return CompletableFuture.completedFuture(CollectionUtils.convert(subchannels, GrpcEndpoint::new));
    }

    @Override
    public CompletionStage<GrpcOutboundResponse> invoke(GrpcOutboundRequest request, GrpcEndpoint endpoint) {
        CompletionStage<GrpcOutboundResponse> stage = new CompletableFuture<>();
        clientCall.start(new ClientCall.Listener() {
            @Override
            public void onMessage(Object message) {
                super.onMessage(message);
            }

        }, new Metadata());
        return stage;
    }

    @Override
    protected GrpcOutboundResponse createResponse(GrpcOutboundRequest request) {
        return null;
    }

    @Override
    protected GrpcOutboundResponse createResponse(GrpcOutboundRequest request, DegradeConfig degradeConfig) {
        return null;
    }

    @Override
    protected GrpcOutboundResponse createResponse(ServiceError error, ErrorPredicate predicate) {
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
