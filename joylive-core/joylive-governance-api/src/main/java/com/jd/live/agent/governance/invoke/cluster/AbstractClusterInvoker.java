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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.AppStatus;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.counter.Counter;
import com.jd.live.agent.governance.policy.live.FaultType;
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
            E extends Endpoint,
            T extends Throwable> CompletionStage<O> execute(LiveCluster<R, O, E, T> cluster,
                                                            InvocationContext context,
                                                            OutboundInvocation<R> invocation,
                                                            ClusterPolicy defaultPolicy) {
        cluster.onStart(invocation.getRequest());
        return invoke(cluster, context, invocation, 0);
    }

    /**
     * Invokes a service method on a cluster of endpoints. This method handles the routing of the
     * request to the appropriate endpoint(s) based on the cluster strategy and an optional predicate
     * that can further refine the selection of endpoints or influence the routing decision.
     *
     * @param <R>        The type of the outbound request that extends {@link OutboundRequest}.
     * @param <O>        The type of the outbound response that extends {@link OutboundResponse}.
     * @param <E>        The type of the endpoint that extends {@link Endpoint}.
     * @param <T>        The type of the throwable that extends {@link Throwable}.
     * @param cluster    The {@link LiveCluster} managing the distribution and processing of the request.
     * @param context    The {@link InvocationContext} providing additional information and context for the invocation.
     * @param invocation The {@link OutboundInvocation} representing the specific request and its routing information.
     * @param counter    The counter that records the current number of retry attempts..
     * @return A {@link CompletionStage} that completes with the result of the service invocation.
     * This future may complete exceptionally if the invocation fails or if no suitable endpoints
     * can be found.
     */
    @SuppressWarnings("unchecked")
    protected <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint,
            T extends Throwable> CompletionStage<O> invoke(LiveCluster<R, O, E, T> cluster,
                                                           InvocationContext context,
                                                           OutboundInvocation<R> invocation,
                                                           int counter) {
        CompletableFuture<O> result = new CompletableFuture<>();
        AppStatus appStatus = context.getAppStatus();
        if (!appStatus.outbound()) {
            T exception = cluster.createUnReadyException(appStatus.getMessage(), invocation.getRequest());
            if (exception != null) {
                result.completeExceptionally(exception);
                return result;
            }
        }
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
                    endpoint = context.route(invocation, v, null, false);
                    E instance = endpoint;
                    Counter statistic = endpoint.getAttribute(Endpoint.ATTRIBUTE_COUNTER);
                    beginCounter(statistic, v);
                    onStart(cluster, request, instance);
                    long startTime = System.currentTimeMillis();
                    cluster.invoke(request, instance).whenComplete((o, r) -> {
                        long elapsed = System.currentTimeMillis() - startTime;
                        if (r != null) {
                            endCounter(statistic, elapsed, false);
                            onException(r, request, instance, cluster, invocation, result);
                        } else if (o.getThrowable() != null) {
                            endCounter(statistic, elapsed, false);
                            onException(o.getThrowable(), request, instance, cluster, invocation, result);
                        } else {
                            endCounter(statistic, elapsed, true);
                            onSuccess(cluster, invocation, o, request, instance, result);
                        }
                    });
                } catch (Throwable e) {
                    onException(e, request, endpoint, cluster, invocation, result);
                }
            } else {
                onException(t, request, null, cluster, invocation, result);
            }
        });
        return result;
    }

    /**
     * Begins the counter process for an outbound request. This method is called before sending the
     * request to the endpoints. It checks if the request has timed out for the specified URI and if
     * so, schedules a timer to clean up the endpoint counters after a 5-second delay. Then, it
     * attempts to start the counter for the request. If the counter cannot be started because it has
     * reached the maximum number of active requests, a fault is thrown with a type of LIMIT.
     *
     * @param <E>       The type of the endpoint that extends {@link Endpoint}.
     * @param counter   The counter instance used to track the number of active requests.
     * @param endpoints The list of endpoint instances to which the request will be sent.
     */
    protected <E extends Endpoint> void beginCounter(Counter counter,
                                                     List<E> endpoints) {
        if (counter != null) {
            counter.getService().tryClean(endpoints);
            if (!counter.begin(0)) {
                throw FaultType.LIMIT.reject("Has reached the maximum number of active requests.");
            }
        }
    }

    /**
     * Handles the start of an invocation process. This method is called before the actual invocation
     * takes place. Subclasses can override this method to perform additional setup or initialization.
     *
     * @param <R>      The type of the outbound request that extends {@link OutboundRequest}.
     * @param <O>      The type of the outbound response that extends {@link OutboundResponse}.
     * @param <E>      The type of the endpoint that extends {@link Endpoint}.
     * @param <T>      The type of the throwable that extends {@link Throwable}.
     * @param cluster  The live cluster context in which the invocation is taking place.
     * @param request  The request that is about to be processed.
     * @param instance The endpoint instance to which the request will be sent.
     */
    protected <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint, T extends Throwable> void onStart(LiveCluster<R, O, E, T> cluster,
                                                                  R request,
                                                                  E instance) {
        cluster.onStartRequest(request, instance);
    }

    /**
     * Ends the counter and records the result of the operation. This method is called after the
     * operation has completed, either successfully or with a failure.
     *
     * @param counter A counter instance used to track the number of active requests.
     * @param elapsed The time taken for the operation to complete, in milliseconds.
     * @param success A flag indicating whether the operation was successful or not.
     */
    protected void endCounter(Counter counter,
                              long elapsed,
                              boolean success) {
        if (counter != null) {
            if (success) {
                counter.success(elapsed);
            } else {
                counter.fail(elapsed);
            }
        }
    }

    /**
     * Handles a successful request response.
     *
     * @param <R>        the type of the outbound request
     * @param <O>        the type of the outbound response
     * @param <E>        the type of the endpoint
     * @param <T>        the type of the throwable
     * @param cluster    the live cluster instance representing the current active cluster
     * @param invocation the outbound invocation instance representing the outbound call
     * @param response   the instance representing the outbound response
     * @param request    the instance representing the outbound request
     * @param endpoint   the endpoint instance representing the endpoint
     * @param result     the CompletableFuture instance representing the result of an asynchronous computation
     */
    protected <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint,
            T extends Throwable> void onSuccess(LiveCluster<R, O, E, T> cluster,
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
     * @param <T>        The type of the throwable that extends {@link Throwable}.
     * @param throwable  The exception that occurred during the invocation.
     * @param request    The request that was being processed when the exception occurred.
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
            E extends Endpoint,
            T extends Throwable> void onException(Throwable throwable,
                                                  R request,
                                                  E endpoint,
                                                  LiveCluster<R, O, E, T> cluster,
                                                  OutboundInvocation<R> invocation,
                                                  CompletableFuture<O> result) {
        O response = cluster.createResponse(throwable, request, endpoint);
        try {
            invocation.onFailure(endpoint, throwable);
            // avoid the live exception class is not recognized in application classloader
            cluster.onError(response.getThrowable(), request, endpoint);
        } catch (Throwable e) {
            logger.warn("Exception occurred when onException, caused by " + e.getMessage(), e);
        } finally {
            result.complete(response);
        }
    }
}
