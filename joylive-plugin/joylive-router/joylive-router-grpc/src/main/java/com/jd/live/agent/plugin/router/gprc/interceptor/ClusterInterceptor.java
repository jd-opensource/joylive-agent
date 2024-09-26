package com.jd.live.agent.plugin.router.gprc.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;

public class ClusterInterceptor extends InterceptorAdaptor {


    private final InvocationContext context;

    private final ObjectParser parser;

    //private final Map<AbstractCluster, GRpcCluster> clusters = new ConcurrentHashMap<>();

    public ClusterInterceptor(InvocationContext context, ObjectParser parser) {
        this.context = context;
        this.parser = parser;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = ctx.getArguments();
        System.out.println("----> enter");
        //GRpcCluster cluster = clusters.computeIfAbsent((AbstractCluster) ctx.getTarget(), c -> new GRpcCluster(c, parser));
//        GRpcOutboundRequest request = new GRpcOutboundRequest((SofaRequest) arguments[0], cluster);
//        if (!request.isSystem() && !request.isDisabled()) {
//            GRpcOutboundInvocation invocation = new GRpcOutboundInvocation(request, new GRpcInvocationContext(context));
//            GRpcOutboundResponse response = cluster.request(invocation, null);
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
