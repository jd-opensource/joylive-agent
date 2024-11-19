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
package com.jd.live.agent.plugin.router.gprc.request;

import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcInboundRequest;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcOutboundRequest;
import io.grpc.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * An interface representing a gRPC request.
 */
public interface GrpcRequest {

    /**
     * A nested class representing an inbound gRPC request.
     */
    class GrpcInboundRequest extends AbstractRpcInboundRequest<ServerCall<?, ?>> implements GrpcRequest {

        private final Metadata headers;

        public GrpcInboundRequest(ServerCall<?, ?> request, Metadata headers) {
            super(request);
            this.headers = headers;
            MethodDescriptor<?, ?> descriptor = request.getMethodDescriptor();
            this.path = descriptor.getServiceName();
            this.method = descriptor.getBareMethodName();
        }

        @Override
        public String getClientIp() {
            SocketAddress address = request.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
            return address instanceof InetSocketAddress ? ((InetSocketAddress) address).getHostString() : null;
        }

        @Override
        public boolean isNativeGroup() {
            return false;
        }
    }

    /**
     * A nested class representing an outbound gRPC request.
     */
    class GrpcOutboundRequest extends AbstractRpcOutboundRequest<ClientCall<?, ?>> implements GrpcRequest {

        public GrpcOutboundRequest(ClientCall<?, ?> request, String serviceName, MethodDescriptor<?, ?> method) {
            super(request);
            this.service = serviceName;
            this.path = method.getServiceName();
            this.method = method.getBareMethodName();
        }

    }
}
