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

import com.jd.live.agent.bootstrap.exception.RejectException.RejectNoProviderException;
import com.jd.live.agent.governance.exception.ErrorName;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcInboundRequest;
import com.jd.live.agent.governance.request.AbstractRpcRequest.AbstractRpcOutboundRequest;
import com.jd.live.agent.governance.request.RoutedRequest;
import com.jd.live.agent.plugin.router.gprc.exception.GrpcException;
import com.jd.live.agent.plugin.router.gprc.exception.GrpcException.GrpcClientException;
import com.jd.live.agent.plugin.router.gprc.loadbalance.LiveDiscovery;
import com.jd.live.agent.plugin.router.gprc.loadbalance.LiveRequest;
import com.jd.live.agent.plugin.router.gprc.loadbalance.LiveRouteResult;
import io.grpc.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

        @Override
        public String getHeader(String key) {
            return headers.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
        }

        @Override
        public List<String> getHeaders(String key) {
            Iterable<String> iterable = headers.getAll(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
            if (iterable == null) {
                return null;
            }
            return StreamSupport.stream(iterable.spliterator(), false)
                    .collect(Collectors.toList());
        }

    }

    /**
     * A nested class representing an outbound gRPC request.
     */
    class GrpcOutboundRequest extends AbstractRpcOutboundRequest<LiveRequest<?, ?>> implements GrpcRequest, RoutedRequest {

        private static final Function<Throwable, ErrorName> GRPC_ERROR_FUNCTION = throwable -> {
            if (throwable instanceof StatusException) {
                return new ErrorName(null, String.valueOf(((StatusException) throwable).getStatus().getCode().value()));
            } else if (throwable instanceof StatusRuntimeException) {
                return new ErrorName(null, String.valueOf(((StatusRuntimeException) throwable).getStatus().getCode().value()));
            } else if (throwable instanceof GrpcException.GrpcServerException) {
                return new ErrorName(null, String.valueOf(((GrpcException.GrpcServerException) throwable).getStatus().getCode().value()));
            }
            return DEFAULT_ERROR_FUNCTION.apply(throwable);
        };

        public GrpcOutboundRequest(LiveRequest<?, ?> request) {
            super(request);
            this.service = LiveDiscovery.getService(request.getPath());
            this.path = request.getPath();
            this.method = request.getMethodName();
        }

        @Override
        public void setHeader(String key, String value) {
            request.setHeader(key, value);
        }

        @Override
        public String getHeader(String key) {
            return request.getHeaders().get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
        }

        @Override
        public List<String> getHeaders(String key) {
            Iterable<String> iterable = request.getHeaders().getAll(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
            if (iterable == null) {
                return null;
            }
            return StreamSupport.stream(iterable.spliterator(), false)
                    .collect(Collectors.toList());
        }

        @SuppressWarnings("unchecked")
        @Override
        public <E extends Endpoint> E getEndpoint() {
            LiveRouteResult result = request.getRouteResult();
            if (result == null) {
                throw RejectNoProviderException.ofService(service);
            } else if (result.isSuccess()) {
                return (E) result.getEndpoint();
            } else if (result.getThrowable() instanceof RuntimeException) {
                throw (RuntimeException) result.getThrowable();
            } else {
                throw new GrpcClientException(result.getThrowable());
            }
        }

        public boolean hasEndpoint() {
            LiveRouteResult result = request.getRouteResult();
            return result != null && result.isSuccess();
        }

        /**
         * Returns the default error name function.
         *
         * @return The default error name function.
         */
        @Override
        public Function<Throwable, ErrorName> getErrorFunction() {
            return GRPC_ERROR_FUNCTION;
        }
    }
}
