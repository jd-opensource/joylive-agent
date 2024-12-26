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

import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.gprc.cluster.GrpcCluster;
import com.jd.live.agent.plugin.router.gprc.exception.GrpcException;
import com.jd.live.agent.plugin.router.gprc.instance.GrpcEndpoint;
import com.jd.live.agent.plugin.router.gprc.request.GrpcRequest.GrpcOutboundRequest;
import com.jd.live.agent.plugin.router.gprc.request.invoke.GrpcInvocation.GrpcOutboundInvocation;
import com.jd.live.agent.plugin.router.gprc.response.GrpcResponse.GrpcOutboundResponse;
import io.grpc.*;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.LoadBalancer.PickSubchannelArgs;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.core.util.CollectionUtils.convert;

/**
 * Live request.
 */
public class LiveRequest extends PickSubchannelArgs {

    public static final CallOptions.Key<LiveRequest> KEY_LIVE_REQUEST = CallOptions.Key.create("x-live-request");

    private static final Map<Class<?>, Optional<Method>> METHODS = new ConcurrentHashMap<>();

    private final MethodDescriptor<?, ?> methodDescriptor;

    private final InvocationContext context;

    private CallOptions callOptions;

    private Metadata headers;

    private Object message;

    private ClientCall clientCall;

    private volatile CompletableFuture<GrpcOutboundResponse> future;

    private final GrpcOutboundRequest request;

    private final GrpcOutboundInvocation invocation;

    private GrpcEndpoint endpoint;

    public LiveRequest(MethodDescriptor<?, ?> methodDescriptor, InvocationContext context) {
        this.methodDescriptor = methodDescriptor;
        this.context = context;
        this.request = new GrpcOutboundRequest(this);
        this.invocation = new GrpcOutboundInvocation(request, context);
    }

    public void setMessage(Object message) {
        this.message = message;
    }

    public InvocationContext getContext() {
        return context;
    }

    public MethodDescriptor<?, ?> getMethodDescriptor() {
        return methodDescriptor;
    }

    public void setCallOptions(CallOptions callOptions) {
        this.callOptions = callOptions;
    }

    public CallOptions getCallOptions() {
        return callOptions;
    }

    public String getMethodName() {
        return methodDescriptor.getBareMethodName();
    }

    public Metadata getHeaders() {
        return headers;
    }

    public void setHeaders(Metadata headers) {
        this.headers = headers;
    }

    public String getPath() {
        return methodDescriptor.getServiceName();
    }

    public ClientCall getClientCall() {
        return clientCall;
    }

    public void setClientCall(ClientCall clientCall) {
        this.clientCall = clientCall;
    }

    public GrpcEndpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(GrpcEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Starts the client call with the specified response listener and metadata headers.
     *
     * @param responseListener The listener for handling response messages and events.
     * @param headers          The metadata headers to be sent with the client call.
     */
    public void start(ClientCall.Listener responseListener, Metadata headers) {
        this.headers = headers;
        clientCall.start(new SimpleForwardingClientCallListener(responseListener) {
            @Override
            public void onMessage(Object message) {
                super.onMessage(message);
                if (future != null) {
                    future.complete(new GrpcOutboundResponse(message));
                }
            }

            @Override
            public void onClose(Status status, Metadata trailers) {
                super.onClose(status, trailers);
                if (!status.isOk() && future != null) {
                    GrpcException exception = new GrpcException(status.asRuntimeException(trailers));
                    GrpcOutboundResponse response = new GrpcOutboundResponse(new ServiceError(exception, true), null);
                    future.complete(response);
                }
            }

        }, headers);
    }

    /**
     * Sends a message and handles the response or any exceptions that may occur.
     *
     * @param message The message to be sent.
     */
    public void sendMessage(Object message) {
        this.message = message;
        GrpcCluster.INSTANCE.invoke(invocation).whenComplete((response, throwable) -> {
            if (clientCall != null && throwable != null && !(throwable instanceof GrpcException)) {
                clientCall.cancel(throwable.getMessage(), throwable);
            }
        });
    }

    /**
     * Sends a message using the client call and returns a CompletionStage for the response.
     *
     * @return A CompletionStage that will be completed with the GrpcOutboundResponse when the message is processed.
     */
    public CompletionStage<GrpcOutboundResponse> sendMessage() {
        CompletableFuture<GrpcOutboundResponse> result = new CompletableFuture<>();
        this.future = result;
        clientCall.sendMessage(message);
        return result;
    }

    public void setHeader(String key, String value) {
        if (headers != null && key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            headers.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value);
        }
    }

    /**
     * Routes the request to an appropriate subchannel from the provided list of subchannels.
     *
     * @param subchannels The list of available subchannels.
     * @return The selected subchannel for routing the request.
     */
    public GrpcEndpoint route(List<LiveSubchannel> subchannels) {
        endpoint = context.route(invocation, convert(subchannels, GrpcEndpoint::new));
        return endpoint;
    }

    /**
     * Retrieves the 'newBuilder' method.
     * If the method is not found or an exception occurs, it returns null.
     *
     * @return The 'newBuilder' method if found, otherwise null.
     */
    public Method getNewBuilder() {
        Class<?> type = methodDescriptor.getResponseMarshaller().getClass();
        Optional<Method> optional = METHODS.computeIfAbsent(type, k -> {
            try {
                String name = k.getName();
                int pos = name.lastIndexOf('$');
                name = name.substring(0, pos);
                k = k.getClassLoader().loadClass(name);
                Method method = k.getMethod("newBuilder");
                method.setAccessible(true);
                return Optional.of(method);
            } catch (Throwable e) {
                return Optional.empty();
            }
        });
        return optional.orElse(null);
    }

    /**
     * Recovers from a degraded state by closing the client call.
     *
     * If the client call is not null, this method will half-close the client call.
     */
    public void onRecover() {
        // recover from degrade
        if (clientCall != null) {
            clientCall.halfClose();
        }
    }

}
