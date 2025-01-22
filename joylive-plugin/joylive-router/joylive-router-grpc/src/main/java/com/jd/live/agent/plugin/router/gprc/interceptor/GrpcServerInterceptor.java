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
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.gprc.request.GrpcRequest.GrpcInboundRequest;
import com.jd.live.agent.plugin.router.gprc.request.invoke.GrpcInvocation.GrpcInboundInvocation;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.*;
import io.grpc.Metadata.Key;

import static com.jd.live.agent.governance.util.ResponseUtils.labelHeaders;
import static com.jd.live.agent.plugin.router.gprc.exception.GrpcInboundThrower.THROWER;

/**
 * A gRPC server interceptor that integrates with the {@code InvocationContext} to enable advanced features like load limiting.
 */
public class GrpcServerInterceptor extends InterceptorAdaptor {

    /**
     * The invocation context used to invoke the next handler in the chain.
     */
    private final InvocationContext context;

    /**
     * Creates a new instance of the {@code GrpcServerInterceptor} class.
     *
     * @param context the invocation context
     */
    public GrpcServerInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        ServerBuilder<?> target = (ServerBuilder<?>) ctx.getTarget();
        target.intercept(new LiveServerInterceptor(context));
    }

    /**
     * Handles an exception that occurred during a server call.
     *
     * @param throwable the exception that was thrown
     * @param call      the server call that encountered the exception
     * @param headers   the metadata associated with the server call
     */
    private static void handleException(Throwable throwable, ServerCall<?, ?> call, Metadata headers) {
        if (throwable instanceof StatusRuntimeException) {
            labelHeaders(throwable.getCause(), (key, value) -> headers.put(Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value));
            call.close(((StatusRuntimeException) throwable).getStatus(), headers);
        } else if (throwable instanceof StatusException) {
            labelHeaders(throwable.getCause(), (key, value) -> headers.put(Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value));
            call.close(((StatusException) throwable).getStatus(), headers);
        } else if (throwable instanceof IllegalArgumentException) {
            labelHeaders(throwable, (key, value) -> headers.put(Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value));
            call.close(Status.INVALID_ARGUMENT.withDescription(throwable.getMessage()), headers);
        } else {
            labelHeaders(throwable, (key, value) -> headers.put(Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value));
            call.close(Status.UNKNOWN.withDescription(throwable.getMessage()), headers);
        }
    }

    /**
     * An interceptor for handling server calls in a live server environment.
     *
     * This interceptor uses an {@link InvocationContext} to manage the invocation lifecycle.
     * It intercepts server calls, processes incoming requests, and handles exceptions.
     */
    private static class LiveServerInterceptor implements ServerInterceptor {

        private final InvocationContext context;

        LiveServerInterceptor(InvocationContext context) {
            this.context = context;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call,
                Metadata headers,
                ServerCallHandler<ReqT, RespT> next) {
            if (!call.getMethodDescriptor().getType().clientSendsOneMessage()) {
                return next.startCall(call, headers);
            }
            GrpcInboundRequest request = new GrpcInboundRequest(call, headers);
            try {
                Object result = !request.isSystem()
                        ? context.inward(new GrpcInboundInvocation(request, context), () -> new LiveServerCallListener<>(next.startCall(call, headers), call, headers))
                        : next.startCall(call, headers);
                return (ServerCall.Listener<ReqT>) result;
            } catch (Throwable e) {
                // convert exception to status exception
                handleException(THROWER.createException(e, request), call, headers);
                return new ServerCall.Listener<ReqT>() {
                    // no-op
                };
            }
        }
    }

    /**
     * A listener for handling server calls that extends {@link SimpleForwardingServerCallListener}.
     *
     * This listener is used to forward server call events to the delegate listener.
     */
    private static class LiveServerCallListener<ReqT, RespT> extends SimpleForwardingServerCallListener<ReqT> {

        private final ServerCall<ReqT, RespT> serverCall;

        private final Metadata headers;

        LiveServerCallListener(ServerCall.Listener<ReqT> delegate, ServerCall<ReqT, RespT> serverCall, Metadata headers) {
            super(delegate);
            this.serverCall = serverCall;
            this.headers = headers;
        }

        @Override
        public void onHalfClose() {
            try {
                super.onHalfClose();
            } catch (Throwable e) {
                handleException(e, serverCall, headers);
            }
        }

        @Override
        public void onReady() {
            try {
                super.onReady();
            } catch (Throwable e) {
                handleException(e, serverCall, headers);
            }
        }
    }
}
