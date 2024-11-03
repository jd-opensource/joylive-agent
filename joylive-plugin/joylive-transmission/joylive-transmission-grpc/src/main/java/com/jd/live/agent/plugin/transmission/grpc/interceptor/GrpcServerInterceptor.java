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
package com.jd.live.agent.plugin.transmission.grpc.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.tag.Label;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.CargoRequire;
import com.jd.live.agent.governance.context.bag.CargoRequires;
import io.grpc.*;

import java.util.List;

public class GrpcServerInterceptor extends InterceptorAdaptor {

    private final CargoRequire require;

    public GrpcServerInterceptor(List<CargoRequire> requires) {
        this.require = new CargoRequires(requires);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        ServerBuilder<?> target = (ServerBuilder<?>) ctx.getTarget();
        target.intercept(new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                if (headers != null) {
                    RequestContext.create().addCargo(require, headers.keys(),
                            name -> Label.parseValue(headers.get(Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER))));
                }
                return next.startCall(new ForwardingServerCall<ReqT, RespT>() {
                    @Override
                    protected ServerCall<ReqT, RespT> delegate() {
                        return call;
                    }

                    @Override
                    public MethodDescriptor<ReqT, RespT> getMethodDescriptor() {
                        return call.getMethodDescriptor();
                    }
                }, headers);
            }
        });
    }
}
