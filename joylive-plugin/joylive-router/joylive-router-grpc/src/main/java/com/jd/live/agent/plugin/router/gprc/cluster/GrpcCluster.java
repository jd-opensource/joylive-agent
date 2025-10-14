package com.jd.live.agent.plugin.router.gprc.cluster;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.AbstractLiveCluster;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint;
import com.jd.live.agent.plugin.router.gprc.loadbalance.LiveDiscovery;
import com.jd.live.agent.plugin.router.gprc.loadbalance.LiveRequest;
import com.jd.live.agent.plugin.router.gprc.request.GrpcRequest.GrpcOutboundRequest;
import com.jd.live.agent.plugin.router.gprc.response.GrpcResponse.GrpcOutboundResponse;
import io.grpc.LoadBalancer.SubchannelPicker;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.plugin.router.gprc.exception.GrpcOutboundThrower.THROWER;

public class GrpcCluster extends AbstractLiveCluster<GrpcOutboundRequest, GrpcOutboundResponse, GrpcEndpoint> {

    private static final Logger logger = LoggerFactory.getLogger(GrpcCluster.class);

    public static final GrpcCluster INSTANCE = new GrpcCluster();

    @Override
    public CompletionStage<List<GrpcEndpoint>> route(GrpcOutboundRequest request) {
        if (!request.hasEndpoint()) {
            // the endpoint maybe null in initialization
            // wait for picker
            SubchannelPicker picker = LiveDiscovery.getSubchannelPicker(request.getService());
            if (picker == null) {
                picker = LiveDiscovery.getSubchannelPicker(request.getService());
            }
            if (picker != null) {
                picker.pickSubchannel(request.getRequest());
            } else {
                logger.warn("[GrpcCluster]Get picker is null, service {}", request.getService());
            }
        }

        // request is already routed
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<GrpcOutboundResponse> invoke(GrpcOutboundRequest request, GrpcEndpoint endpoint) {
        LiveRequest<?, ?> req = request.getRequest();
        return req.sendMessage();
    }

    @Override
    public void onRetry(GrpcOutboundRequest request, int retries) {
        if (retries > 0) {
            request.getRequest().onRetry();
        }
    }

    @Override
    public void onRecover(GrpcOutboundResponse response, GrpcOutboundRequest request, GrpcEndpoint endpoint) {
        request.getRequest().onRecover();
    }

    @Override
    protected boolean isValid(DegradeConfig config) {
        return config.bodyLength() > 0 && super.isValid(config);
    }

    @Override
    protected GrpcOutboundResponse createResponse(GrpcOutboundRequest request) {
        return new GrpcOutboundResponse(null);
    }

    @Override
    protected GrpcOutboundResponse createResponse(GrpcOutboundRequest request, DegradeConfig degradeConfig) {
        try {
            Object response = request.getRequest().parse(degradeConfig.getResponseBytes());
            Metadata metadata = new Metadata();
            degradeConfig.foreach((key, value) -> metadata.put(Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value));
            return new GrpcOutboundResponse(response, metadata);
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
