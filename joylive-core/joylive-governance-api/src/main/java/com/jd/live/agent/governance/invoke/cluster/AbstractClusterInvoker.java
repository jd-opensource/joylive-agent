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
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

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
    @SuppressWarnings("unchecked")
    protected <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> CompletionStage<O> invoke(LiveCluster<R, O, E> cluster, OutboundInvocation<R> invocation, int counter) {
        CompletableFuture<O> result = new CompletableFuture<>();
        InvocationContext context = invocation.getContext();
        R request = invocation.getRequest();
        List<? extends Endpoint> instances = invocation.getInstances();
        if (counter > 0) {
            invocation.reset();
        }
        CompletionStage<List<E>> discoveryStage = instances == null || instances.isEmpty() || counter > 0
                ? cluster.route(request)
                : CompletableFuture.completedFuture((List<E>) instances);
        discoveryStage.whenComplete((v, t) -> {
            if (t == null) {
                E endpoint = null;
                try {
                    endpoint = context.route(invocation, v);
                    E instance = endpoint;
                    onStartRequest(cluster, request, endpoint);
                    CompletionStage<O> stage = context.outbound(invocation, endpoint, () -> cluster.invoke(request, instance));
                    stage.whenComplete((o, r) -> {
                        if (r != null) {
                            logger.error("Exception occurred when invoke, caused by " + r.getMessage(), r);
                            onException(cluster, invocation, o, new ServiceError(r, false), instance, result);
                        } else if (o.getError() != null) {
                            onException(cluster, invocation, o, o.getError(), instance, result);
                        } else {
                            onSuccess(cluster, invocation, o, request, instance, result);
                        }
                    });
                } catch (Throwable e) {
                    logger.error("Exception occurred when routing, caused by " + e.getMessage(), e);
                    onException(cluster, invocation, null, new ServiceError(e, false), endpoint, result);
                }
            } else {
                logger.error("Exception occurred when service discovery, caused by " + t.getMessage(), t);
                onException(cluster, invocation, null, new ServiceError(t, false), null, result);
            }
        });
        return result;
    }

    /**
     * Handles the start of an invocation process. This method is called before the actual invocation
     * takes place. Subclasses can override this method to perform additional setup or initialization.
     *
     * @param <R>      The type of the outbound request that extends {@link OutboundRequest}.
     * @param <O>      The type of the outbound response that extends {@link OutboundResponse}.
     * @param <E>      The type of the endpoint that extends {@link Endpoint}.
     * @param cluster  The live cluster context in which the invocation is taking place.
     * @param request  The request that is about to be processed.
     * @param instance The endpoint instance to which the request will be sent.
     */
    protected <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> void onStartRequest(LiveCluster<R, O, E> cluster, R request, E instance) {
        cluster.onStartRequest(request, instance);
    }

    /**
     * Handles a successful request response.
     *
     * @param <R>        the type of the outbound request
     * @param <O>        the type of the outbound response
     * @param <E>        the type of the endpoint
     * @param cluster    the live cluster instance representing the current active cluster
     * @param invocation the outbound invocation instance representing the outbound call
     * @param response   the instance representing the outbound response
     * @param request    the instance representing the outbound request
     * @param endpoint   the endpoint instance representing the endpoint
     * @param result     the CompletableFuture instance representing the result of an asynchronous computation
     */
    protected <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> void onSuccess(LiveCluster<R, O, E> cluster,
                                               OutboundInvocation<R> invocation,
                                               O response,
                                               R request,
                                               E endpoint,
                                               CompletableFuture<O> result) {
        try {
            invocation.onSuccess(endpoint, response);
            cluster.onSuccess(response, request, endpoint);
        } catch (Throwable e) {
            logger.warn("Exception occurred when onSuccess, caused by " + e.getMessage(), e);
        } finally {
            result.complete(response);
        }
    }

    /**
     * Handles exceptions that occur during the invocation process. Subclasses must implement this
     * method to define custom exception handling logic, which may include logging, retrying the
     * invocation, or failing the request.
     *
     * @param <R>        The type of the outbound request that extends {@link OutboundRequest}.
     * @param <O>        The type of the outbound response that extends {@link OutboundResponse}.
     * @param <E>        The type of the endpoint that extends {@link Endpoint}.
     * @param error      The exception that occurred during the invocation.
     * @param endpoint   The endpoint to which the request was being sent, may be null if the exception
     *                   occurred before an endpoint was selected.
     * @param cluster    The live cluster context in which the invocation was taking place.
     * @param invocation The {@link OutboundInvocation} representing the specific request and its routing information.
     * @param result     The {@link CompletableFuture} that should be completed to signal the outcome of the
     *                   invocation. Implementers can complete this future exceptionally or with a default
     *                   response.
     */
    protected <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> void onException(LiveCluster<R, O, E> cluster,
                                                 OutboundInvocation<R> invocation,
                                                 O response,
                                                 ServiceError error,
                                                 E endpoint,
                                                 CompletableFuture<O> result) {

        R request = invocation.getRequest();
        Throwable cause = error.getThrowable();
        boolean serverError = error.isServerError();
        response = response != null && serverError ? response : cluster.createResponse(cause, request, endpoint);
        error = response.getError();
        try {
            invocation.onFailure(endpoint, cause);
            if (error == null) {
                // Request was handled successfully by degrade
                cluster.onSuccess(response, request, endpoint);
            } else if (cause instanceof LiveException) {
                // Request did not go off box
                cluster.onDiscard(request);
            } else {
                // Request reached the server but failed
                cluster.onError(error.getThrowable(), request, endpoint);
            }
        } catch (Throwable e) {
            logger.warn("Exception occurred when onException, caused by " + e.getMessage(), e);
        } finally {
            if (error == null) {
                result.complete(response);
            } else {
                result.completeExceptionally(error.getThrowable());
            }
        }
    }
}
