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
import com.jd.live.agent.plugin.router.gprc.exception.GrpcStatus;
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
import java.util.function.Supplier;

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

    private Supplier<ClientCall> supplier;

    private Metadata headers;

    private ClientCall.Listener responseListener;

    private Object message;

    private Integer numMessages;

    private Boolean messageCompression;

    private ClientCall clientCall;

    private CompletableFuture<GrpcOutboundResponse> future;

    private final GrpcOutboundRequest request;

    private final GrpcOutboundInvocation invocation;

    private GrpcEndpoint endpoint;

    public LiveRequest(MethodDescriptor<?, ?> methodDescriptor, InvocationContext context) {
        this.methodDescriptor = methodDescriptor;
        this.context = context;
        this.request = new GrpcOutboundRequest(this);
        this.invocation = new GrpcOutboundInvocation(request, context);
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }

    public CompletionStage<?> getFuture() {
        return future;
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

    public void setSupplier(Supplier<ClientCall> supplier) {
        this.supplier = supplier;
    }

    public void start(ClientCall.Listener responseListener, Metadata headers) {
        this.headers = headers;
        this.responseListener = responseListener;
    }

    public void request(int numMessages) {
        this.numMessages = numMessages;
    }

    public void halfClose() {
        if (clientCall != null) {
            clientCall.halfClose();
        }
    }

    public void setMessageCompression(boolean enabled) {
        messageCompression = enabled;
        if (clientCall != null) {
            clientCall.setMessageCompression(enabled);
        }
    }

    public boolean isReady() {
        return clientCall != null && clientCall.isReady();
    }

    public Attributes getAttributes() {
        return clientCall == null ? null : clientCall.getAttributes();
    }

    public void cancel(String message, Throwable throwable) {
        if (clientCall != null) {
            clientCall.cancel(message, throwable);
        }
    }

    public void sendMessage(Object message) {
        this.message = message;
        // wrap cluster to invoke & handle void response
        GrpcCluster cluster = GrpcCluster.INSTANCE;
        GrpcOutboundResponse response = cluster.request(invocation);
        ServiceError error = response.getError();
        if (error != null && error.hasException()) {
            if (error.getThrowable() instanceof RuntimeException) {
                throw (RuntimeException) error.getThrowable();
            } else {
                throw GrpcStatus.createException(error.getThrowable()).asRuntimeException(new Metadata());
            }
        }
    }

    /**
     * Sends a message using the client call and returns a CompletionStage for the response.
     *
     * @return A CompletionStage that will be completed with the GrpcOutboundResponse when the message is processed.
     */
    public CompletionStage<GrpcOutboundResponse> sendMessage() {
        clientCall.sendMessage(message);
        return future;
    }

    public GrpcEndpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(GrpcEndpoint endpoint) {
        this.endpoint = endpoint;
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

    public ClientCall getClientCall() {
        return clientCall;
    }

    public ClientCall newClientCall() {
        return supplier.get();
    }

    /**
     * Closes the half of the connection that sends data, indicating that no more messages will be sent.
     * This method is called when the operation is discarded.
     */
    @SuppressWarnings("unchecked")
    public void onStart() {
        // called by GrpcCluster.route
        future = new CompletableFuture<>();
        clientCall = newClientCall();
        // wrap response listener to handle response & error
        clientCall.start(new SimpleForwardingClientCallListener(responseListener) {
            @Override
            public void onMessage(Object message) {
                super.onMessage(message);
                future.complete(new GrpcOutboundResponse(message));
            }

            @Override
            public void onClose(Status status, Metadata trailers) {
                super.onClose(status, trailers);
                if (!status.isOk()) {
                    future.completeExceptionally(status.asRuntimeException(trailers));
                }
            }

        }, headers);
        if (numMessages != null) {
            clientCall.request(numMessages);
        }
        if (messageCompression != null) {
            clientCall.setMessageCompression(messageCompression);
        }
    }

    /**
     * Closes the half of the connection that sends data, indicating that no more messages will be sent.
     * This method is called when the operation is successful.
     */
    public void onSuccess() {
        if (clientCall != null) {
            clientCall.halfClose();
        }
    }

    /**
     * Cancels the client call with the specified error message and throwable.
     * This method is called when an error occurs.
     *
     * @param throwable The throwable representing the error.
     */
    public void onError(Throwable throwable) {
        if (clientCall != null) {
            clientCall.cancel(throwable.getMessage(), throwable);
        }
    }

    /**
     * Closes the half of the connection that sends data, indicating that no more messages will be sent.
     * This method is called when the operation is discarded.
     */
    public void onDiscard() {
        if (clientCall != null) {
            clientCall.halfClose();
        }
    }

}
