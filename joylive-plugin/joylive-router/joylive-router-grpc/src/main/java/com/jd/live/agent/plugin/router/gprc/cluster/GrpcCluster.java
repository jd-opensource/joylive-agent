package com.jd.live.agent.plugin.router.gprc.cluster;

import com.jd.live.agent.core.util.CollectionUtils;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.AbstractLiveCluster;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint;
import com.jd.live.agent.plugin.router.gprc.loadbalance.LiveDiscovery;
import com.jd.live.agent.plugin.router.gprc.loadbalance.LivePickerAdvice;
import com.jd.live.agent.plugin.router.gprc.loadbalance.LiveSubchannel;
import com.jd.live.agent.plugin.router.gprc.request.GrpcRequest.GrpcOutboundRequest;
import com.jd.live.agent.plugin.router.gprc.response.GrpcResponse.GrpcOutboundResponse;
import io.grpc.ClientCall;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


public class GrpcCluster extends AbstractLiveCluster<GrpcOutboundRequest, GrpcOutboundResponse, GrpcEndpoint> {

    private ClientCall clientCall;

    private LivePickerAdvice advice;

    private CompletionStage<GrpcOutboundResponse> stage;

    public GrpcCluster(ClientCall clientCall, LivePickerAdvice advice, CompletionStage<GrpcOutboundResponse> stage) {
        this.clientCall = clientCall;
        this.advice = advice;
        this.stage = stage;
    }

    @Override
    public CompletionStage<List<GrpcEndpoint>> route(GrpcOutboundRequest request) {
        List<LiveSubchannel> subchannels = LiveDiscovery.getSubchannel(request.getService());
        return CompletableFuture.completedFuture(CollectionUtils.convert(subchannels, GrpcEndpoint::new));
    }

    @Override
    public CompletionStage<GrpcOutboundResponse> invoke(GrpcOutboundRequest request, GrpcEndpoint endpoint) {
        advice.setSubchannel(endpoint.getSubchannel());
        clientCall.sendMessage(request.getRequest());
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
