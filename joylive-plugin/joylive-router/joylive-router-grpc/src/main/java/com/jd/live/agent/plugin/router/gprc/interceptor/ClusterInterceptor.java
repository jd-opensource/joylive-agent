package com.jd.live.agent.plugin.router.gprc.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
//import com.jd.live.agent.governance.invoke.OutboundInvocation;
//import com.jd.live.agent.governance.response.ServiceError;
import com.jd.live.agent.plugin.router.gprc.cluster.GrpcCluster;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//import com.jd.live.agent.plugin.router.gprc.request.GrpcRequest;
//import com.jd.live.agent.plugin.router.gprc.request.GrpcRequest.GrpcOutboundRequest;
//import com.jd.live.agent.plugin.router.gprc.response.GrpcResponse;

public class ClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final ObjectParser parser;

    private final Map<SocketAddress, GrpcCluster> clusters = new ConcurrentHashMap<>();

    public ClusterInterceptor(InvocationContext context, ObjectParser parser) {
        this.context = context;
        this.parser = parser;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = ctx.getArguments();
//        GrpcCluster cluster = clusters.computeIfAbsent((SocketAddress) ctx.getTarget(), GrpcCluster::new);
//        GrpcOutboundRequest request = new GrpcOutboundRequest((GrpcRequest) arguments[0], cluster);
//        if (!request.isSystem() && !request.isDisabled()) {
//            OutboundInvocation.RpcOutboundInvocation invocation = new OutboundInvocation.RpcOutboundInvocation(request, context);
//            GrpcResponse.GrpcOutboundResponse response = cluster.request(invocation, null);
//            ServiceError error = response.getError();
//            if (error != null && !error.isServerError()) {
//                mc.setThrowable(error.getThrowable());
//            } else {
//                mc.setResult(response.getResponse());
//            }
//            mc.setSkip(true);
//        }
    }

}
