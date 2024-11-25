/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.plugin.router.gprc.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint;
import com.jd.live.agent.plugin.router.gprc.loadbalance.LiveDiscovery;
import com.jd.live.agent.plugin.router.gprc.loadbalance.LivePickerAdvice;
import com.jd.live.agent.plugin.router.gprc.request.GrpcRequest.GrpcOutboundRequest;
import com.jd.live.agent.plugin.router.gprc.request.invoke.GrpcInvocation.GrpcOutboundInvocation;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;

import static com.jd.live.agent.core.util.CollectionUtils.convert;

/**
 * ClientInterceptorsInterceptor
 */
public class ClientInterceptorsInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public ClientInterceptorsInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        mc.setResult(new LiveChannel(context, mc.getResult()));
    }

    /**
     * A private static class that extends the Channel class to provide a live channel implementation.
     */
    private static class LiveChannel extends Channel {

        private final InvocationContext context;

        private final Channel channel;

        LiveChannel(InvocationContext context, Channel channel) {
            this.context = context;
            this.channel = channel;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> newCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions) {
            String serviceName = LiveDiscovery.getService(method.getServiceName());
            GrpcOutboundRequest request = new GrpcOutboundRequest(null, serviceName, method);
            GrpcOutboundInvocation invocation = new GrpcOutboundInvocation(request, context);
            LivePickerAdvice advice = new LivePickerAdvice(channels -> {
                // lazy route.
                GrpcEndpoint endpoint = context.route(invocation, convert(channels, GrpcEndpoint::new));
                return endpoint.getSubchannel();
            });
            callOptions = callOptions.withOption(LivePickerAdvice.KEY_PICKER_ADVICE, advice);
            return channel.newCall(method, callOptions);
        }

        @Override
        public String authority() {
            return channel.authority();
        }
    }
}
