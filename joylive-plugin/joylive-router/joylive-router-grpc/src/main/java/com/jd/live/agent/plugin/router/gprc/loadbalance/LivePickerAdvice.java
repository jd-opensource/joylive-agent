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
package com.jd.live.agent.plugin.router.gprc.loadbalance;

import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint;
import com.jd.live.agent.plugin.router.gprc.request.GrpcRequest.GrpcOutboundRequest;
import com.jd.live.agent.plugin.router.gprc.request.invoke.GrpcInvocation.GrpcOutboundInvocation;
import io.grpc.CallOptions;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.convert;

/**
 * Represents advice for live picker, which includes a subchannel and an election function to determine the active subchannel.
 */
public class LivePickerAdvice {

    public static final CallOptions.Key<LivePickerAdvice> KEY_PICKER_ADVICE = CallOptions.Key.create("x-picker-advice");

    private final MethodDescriptor<?, ?> method;

    private final InvocationContext context;

    private Object message;

    private Metadata headers;

    private LiveSubchannel subchannel;

    public LivePickerAdvice(MethodDescriptor<?, ?> method, InvocationContext context) {
        this.method = method;
        this.context = context;
    }

    public void setHeaders(Metadata headers) {
        this.headers = headers;
    }

    public void setMessage(Object message) {
        this.message = message;
    }

    public void setSubchannel(LiveSubchannel subchannel) {
        this.subchannel = subchannel;
    }

    public LiveSubchannel elect(List<LiveSubchannel> subchannels) {
        if (subchannel != null) {
            return subchannel;
        }
        String serviceName = LiveDiscovery.getService(method.getServiceName());
        GrpcOutboundRequest request = new GrpcOutboundRequest(message, headers, method, serviceName);
        GrpcOutboundInvocation invocation = new GrpcOutboundInvocation(request, context);
        GrpcEndpoint endpoint = context.route(invocation, convert(subchannels, GrpcEndpoint::new));
        return endpoint.getSubchannel();
    }

}
