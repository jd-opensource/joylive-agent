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
package com.jd.live.agent.governance.invoke.cluster;

import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.request.RoutedRequest;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse.Asyncable;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Abstract implementation of {@link ClusterInvoker} that manages the invocation of services
 * across a cluster of endpoints. This class provides a common framework for routing requests,
 * handling failures, and integrating with different cluster strategies.
 */
public abstract class AbstractClusterInvoker implements ClusterInvoker {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClusterInvoker.class);

    @Override
    public <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> CompletionStage<O> execute(LiveCluster<R, O, E> cluster,
                                                           OutboundInvocation<R> invocation,
                                                           ClusterPolicy defaultPolicy) {
        cluster.onStart(invocation.getRequest());
        return invoke(cluster, invocation, 0);
    }

    /**
     * Invokes a service method on a cluster of endpoints. This method handles the routing of the
     * request to the appropriate endpoint(s) based on the cluster strategy and an optional predicate
     * that can further refine the selection of endpoints or influence the routing decision.
     *
     * @param <R>        The type of the outbound request that extends {@link OutboundRequest}.
     * @param <O>        The type of the outbound response that extends {@link OutboundResponse}.
     * @param <E>        The type of the endpoint that extends {@link Endpoint}.
     * @param cluster    The {@link LiveCluster} managing the distribution and processing of the request.
     * @param invocation The {@link OutboundInvocation} representing the specific request and its routing information.
     * @param counter    The counter that records the current number of retry attempts..
     * @return A {@link CompletionStage} that completes with the result of the service invocation.
     * This future may complete exceptionally if the invocation fails or if no suitable endpoints
     * can be found.
     */
    protected <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> CompletionStage<O> invoke(LiveCluster<R, O, E> cluster, OutboundInvocation<R> invocation, int counter) {
        return new Invoker<>(cluster, invocation, counter).execute();
    }

    /**
     * Handles cluster service invocation with discovery, routing, and error handling.
     * Manages the complete request lifecycle from endpoint discovery to response completion.
     */
    @Getter
    protected static class Invoker<R extends OutboundRequest, O extends OutboundResponse, E extends Endpoint> {

        protected final LiveCluster<R, O, E> cluster;
        protected final OutboundInvocation<R> invocation;
        protected final int counter;
        protected final InvocationContext context;
        protected final R request;

        Invoker(final LiveCluster<R, O, E> cluster, final OutboundInvocation<R> invocation, final int counter) {
            this.cluster = cluster;
            this.invocation = invocation;
            this.counter = counter;
            this.context = invocation.getContext();
            this.request = invocation.getRequest();
        }

        /**
         * Executes the complete invocation pipeline: discover endpoints, route request,
         * invoke service, and handle response/errors.
         *
         * @return completion stage with the service response
         */
        public CompletionStage<O> execute() {
            return route().thenCompose(this::invoke).handle(this::complete);
        }

        /**
         * Routes request to target endpoint using context routing or pre-routed request.
         *
         * @return void completion stage when routing is complete
         */
        @SuppressWarnings("unchecked")
        protected CompletionStage<E> route() {
            if (request instanceof RoutedRequest) {
                return CompletableFuture.completedFuture(((RoutedRequest) request).getEndpoint());
            } else if (invocation.isEmpty() || counter > 0) {
                // discover and route
                return cluster.route(request).thenApply(v -> context.route(invocation, v));
            } else {
                return CompletableFuture.completedFuture(context.route(invocation, (List<E>) invocation.getInstances()));
            }
        }

        /**
         * Invokes service on selected endpoint with async support.
         *
         * @param endpoint Selected endpoint for service invocation
         * @return void completion stage when invocation completes
         */
        @SuppressWarnings("unchecked")
        protected CompletionStage<InvokeResult<O, E>> invoke(E endpoint) {
            cluster.onStartRequest(request, endpoint);
            return context
                    .outbound(invocation, endpoint, () -> cluster.invoke(request, endpoint))
                    .thenCompose(r -> {
                        CompletionStage<Object> stage = r instanceof Asyncable ? ((Asyncable) r).getFuture() : null;
                        if (stage == null) {
                            return CompletableFuture.completedFuture(new InvokeResult<>((O) r, endpoint));
                        }
                        return stage
                                .thenApply(v -> new InvokeResult<>((O) r, endpoint))
                                .exceptionally(e -> new InvokeResult<>(new ServiceError(e, false), endpoint));
                    })
                    .exceptionally(e -> new InvokeResult<>(new ServiceError(e, false), endpoint));
        }

        /**
         * Handles invocation completion with success/error callbacks.
         *
         * @param result    invoke result
         * @param throwable exception if invocation failed, null otherwise
         * @return final response
         */
        protected O complete(InvokeResult<O, E> result, Throwable throwable) {
            if (throwable != null) {
                // result is null when exception occurred
                result = new InvokeResult<>(new ServiceError(throwable, false));
            }
            try {
                if (result.hasException()) {
                    // degrade when exception occurred, will create exception response.
                    onException(result);
                } else {
                    onSuccess(result);
                }
            } catch (Throwable e) {
                logger.warn("Exception occurred when complete, caused by " + e.getMessage(), e);
            }
            return result.response;
        }

        /**
         * Handles successful response with success callbacks to invocation and cluster.
         * @param result Invoke result
         */
        protected void onSuccess(InvokeResult<O, E> result) {
            invocation.onSuccess(result.endpoint, result.response);
            cluster.onSuccess(result.response, request, result.endpoint);
        }

        /**
         * Handles service errors with appropriate callbacks and error categorization.
         *
         * @param result Invoke result
         */
        protected void onException(InvokeResult<O, E> result) {
            Throwable e = result.error.getThrowable();
            logger.error("Exception occurred, caused by " + e.getMessage(), e instanceof LiveException ? null : e);
            if (result.response == null || !result.error.isServerError()) {
                // convert client response error.
                result.response = cluster.createResponse(e, request, result.endpoint);
                // degrade maybe changed error
                result.error = result.response.getError();
            }
            invocation.onFailure(result.endpoint, e);
            if (result.error == null) {
                // Request was recover successfully by degrade
                invocation.onRecover();
                cluster.onRecover(result.response, request, result.endpoint);
            } else if (e instanceof LiveException) {
                // Request did not go off box
                cluster.onDiscard(request);
            } else {
                // Request reached the server but failed
                cluster.onError(result.error.getThrowable(), request, result.endpoint);
            }
        }
    }

    protected static class InvokeResult<O extends OutboundResponse, E extends Endpoint> {

        protected O response;
        protected E endpoint;
        protected ServiceError error;

        public InvokeResult(ServiceError error) {
            this.error = error;
        }

        public InvokeResult(ServiceError error, E endpoint) {
            this.error = error;
            this.endpoint = endpoint;
        }

        public InvokeResult(O response, E endpoint) {
            this.response = response;
            this.endpoint = endpoint;
            this.error = response.getError();
        }

        public boolean hasException() {
            return error != null && error.hasException();
        }
    }
}
