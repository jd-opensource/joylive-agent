package com.jd.live.agent.plugin.router.springcloud.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.plugin.router.springcloud.v2.cluster.ReactiveCluster;
import com.jd.live.agent.plugin.router.springcloud.v2.request.ReactiveClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v2.response.ReactiveClusterResponse;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: yuanjinzhong
 * @date: 2025/1/2 20:02
 * @description:
 */
public class ReactiveClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final Map<ExchangeFilterFunction, ReactiveCluster> clusters = new ConcurrentHashMap<>();

    public ReactiveClusterInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = ctx.getArguments();
        ClientRequest request = (ClientRequest) arguments[0];
        if (context.isFlowControlEnabled()) {
            ReactiveCluster cluster = clusters.computeIfAbsent((ExchangeFilterFunction) ctx.getTarget(), ReactiveCluster::new);

            ReactiveClusterRequest clusterRequest = new ReactiveClusterRequest(request, cluster.getLoadBalancerFactory(), (ExchangeFunction) arguments[1]);
            HttpOutboundInvocation<ReactiveClusterRequest> invocation = new HttpOutboundInvocation<>(clusterRequest, context);

            CompletionStage<ReactiveClusterResponse> response = cluster.invoke(invocation);

            CompletableFuture<ClientResponse> future = response.toCompletableFuture().thenApply(ReactiveClusterResponse::getResponse);
            Mono<ClientResponse> mono = Mono.fromFuture(future);
            // mono will be consumed later by the processing pipeline in mc
            mc.setResult(mono);
            mc.setSkip(true);
        } else {
            // only for live & lane
            String serviceName = request.url().getHost();
            RequestContext.setAttribute(Carrier.ATTRIBUTE_SERVICE_ID, serviceName);
            RequestContext.setAttribute(Carrier.ATTRIBUTE_REQUEST, request);
        }



    }
}
