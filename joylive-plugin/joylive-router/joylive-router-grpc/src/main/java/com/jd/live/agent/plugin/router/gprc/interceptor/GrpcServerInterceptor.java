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
import io.grpc.*;

import java.util.concurrent.Callable;

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
        target.intercept(new ServerInterceptor() {
            @SuppressWarnings("unchecked")
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    Metadata headers,
                    ServerCallHandler<ReqT, RespT> next) {

                Callable<Object> callable = () -> next.startCall(new ForwardingServerCall<ReqT, RespT>() {
                    @Override
                    protected ServerCall<ReqT, RespT> delegate() {
                        return call;
                    }

                    @Override
                    public MethodDescriptor<ReqT, RespT> getMethodDescriptor() {
                        return call.getMethodDescriptor();
                    }
                }, headers);

                GrpcInboundRequest request = new GrpcInboundRequest(call, headers);
                try {
                    Object result = !request.isSystem()
                            ? context.inward(new GrpcInboundInvocation(request, context), callable)
                            : callable.call();
                    return (ServerCall.Listener<ReqT>) result;
                } catch (Throwable e) {
                    Throwable throwable = THROWER.createException(e, request);
                    StatusRuntimeException t = throwable instanceof StatusRuntimeException
                            ? (StatusRuntimeException) throwable
                            : Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException(new Metadata());
                    call.close(t.getStatus(), t.getTrailers());
                    return new ServerCall.Listener<ReqT>() {
                        // no-op
                    };
                }
            }
        });
    }
}
