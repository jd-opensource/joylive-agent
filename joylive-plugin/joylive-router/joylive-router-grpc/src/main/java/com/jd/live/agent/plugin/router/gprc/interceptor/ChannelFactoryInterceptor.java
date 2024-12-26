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
import com.jd.live.agent.plugin.router.gprc.loadbalance.LiveRequest;
import io.grpc.*;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.MethodDescriptor.MethodType;

/**
 * ClientInterceptorsInterceptor
 */
public class ChannelFactoryInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public ChannelFactoryInterceptor(InvocationContext context) {
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
            if (method.getType() != MethodType.UNARY) {
                // This is not a Unary RPC method
                return channel.newCall(method, callOptions);
            }
            LiveRequest request = new LiveRequest(method, context);
            CallOptions options = callOptions.withOption(LiveRequest.KEY_LIVE_REQUEST, request);
            request.setCallOptions(options);
            request.setClientCall(channel.newCall(method, options));
            return context.isFlowControlEnabled() ? new FlowControlClientCall<>(request) : new LiveClientCall<>(request);
        }

        @Override
        public String authority() {
            return channel.authority();
        }
    }

    /**
     * A custom client call that extends SimpleForwardingClientCall to provide additional functionality
     * such as handling headers, messages, and responses using LivePickerAdvice.
     *
     * @param <ReqT>  The type of the request message.
     * @param <RespT> The type of the response message.
     */
    private static class LiveClientCall<ReqT, RespT> extends SimpleForwardingClientCall<ReqT, RespT> {

        private final LiveRequest request;

        LiveClientCall(LiveRequest request) {
            super(request.getClientCall());
            this.request = request;
        }

        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
            request.setHeaders(headers);
            super.start(responseListener, headers);
        }

        @Override
        public void sendMessage(ReqT message) {
            request.setMessage(message);
            super.sendMessage(message);
        }
    }

    /**
     * A custom client call that extends SimpleForwardingClientCall to provide additional functionality
     * such as handling headers, messages, and responses using LivePickerAdvice.
     *
     * @param <ReqT>  The type of the request message.
     * @param <RespT> The type of the response message.
     */
    private static class FlowControlClientCall<ReqT, RespT> extends SimpleForwardingClientCall<ReqT, RespT> {

        private final LiveRequest request;

        FlowControlClientCall(LiveRequest request) {
            // delay create client call
            super(request.getClientCall());
            this.request = request;
        }

        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
            request.start(responseListener, headers);
        }

        @Override
        public void sendMessage(ReqT message) {
            request.sendMessage(message);
        }
    }
}
