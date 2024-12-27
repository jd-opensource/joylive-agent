package com.jd.live.agent.plugin.router.gprc.cluster;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.AbstractLiveCluster;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint;
import com.jd.live.agent.plugin.router.gprc.loadbalance.LiveDiscovery;
import com.jd.live.agent.plugin.router.gprc.request.GrpcRequest.GrpcOutboundRequest;
import com.jd.live.agent.plugin.router.gprc.response.GrpcResponse.GrpcOutboundResponse;
import io.grpc.LoadBalancer.SubchannelPicker;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.plugin.router.gprc.exception.GrpcOutboundThrower.THROWER;

public class GrpcCluster extends AbstractLiveCluster<GrpcOutboundRequest, GrpcOutboundResponse, GrpcEndpoint> {

    public static final GrpcCluster INSTANCE = new GrpcCluster();

    @Override
    public CompletionStage<List<GrpcEndpoint>> route(GrpcOutboundRequest request) {
        // start channel
        if (request.getEndpoint() == null) {
            // the endpoint maybe null in initialization
            // wait for picker
            SubchannelPicker picker = LiveDiscovery.getSubchannelPicker(request.getService());
            if (picker != null) {
                picker.pickSubchannel(request.getRequest());
            }
        }

        // request is already routed
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<GrpcOutboundResponse> invoke(GrpcOutboundRequest request, GrpcEndpoint endpoint) {
        return request.getRequest().sendMessage();
    }

    @Override
    public void onRecover(GrpcOutboundResponse response, GrpcOutboundRequest request, GrpcEndpoint endpoint) {
        request.getRequest().onRecover();
    }

    @Override
    protected GrpcOutboundResponse createResponse(GrpcOutboundRequest request) {
        return new GrpcOutboundResponse(null);
    }

    @Override
    protected GrpcOutboundResponse createResponse(GrpcOutboundRequest request, DegradeConfig degradeConfig) {
        Method method = request.getRequest().getNewBuilder();
        if (method == null) {
            Throwable throwable = createException(new LiveException("method newBuilder is not found"), request);
            return createResponse(new ServiceError(throwable, false), getRetryPredicate());
        } else if (method.getReturnType() == Void.class) {
            return new GrpcOutboundResponse(null);
        }
        try {
            Message.Builder builder = (Message.Builder) method.invoke(null);
            JsonFormat.parser().merge(new StringReader(degradeConfig.getResponseBody()), builder);
            Object response = builder.build();
            return new GrpcOutboundResponse(response);
        } catch (Throwable e) {
            return createResponse(new ServiceError(createException(e, request), false), getRetryPredicate());
        }
    }

    @Override
    protected GrpcOutboundResponse createResponse(ServiceError error, ErrorPredicate predicate) {
        return new GrpcOutboundResponse(error, predicate);
    }

    @Override
    public Throwable createException(Throwable throwable, GrpcOutboundRequest request) {
        return THROWER.createException(throwable, request);
    }

    @Override
    public Throwable createException(Throwable throwable, GrpcOutboundRequest request, GrpcEndpoint endpoint) {
        return THROWER.createException(throwable, request, endpoint);
    }

    @Override
    public Throwable createException(Throwable throwable, OutboundInvocation<GrpcOutboundRequest> invocation) {
        return THROWER.createException(throwable, invocation);
    }


}
