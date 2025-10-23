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

import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.exception.OutboundThrower;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.request.StickyRequest;
import com.jd.live.agent.governance.request.StickySessionFactory;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static com.jd.live.agent.core.util.ExceptionUtils.getCause;

/**
 * Defines the behavior of a live cluster capable of routing and invoking outbound requests.
 * <p>
 * This interface extends {@link StickyRequest} to support sticky routing behavior, where
 * requests can be consistently routed to the same endpoint based on a sticky identifier.
 * It is designed to be generic, supporting various types of outbound requests, responses,
 * endpoints, and exceptions.
 * </p>
 *
 * @param <R> The type of the outbound request.
 * @param <O> The type of the outbound response.
 * @param <E> The type of the endpoint to which requests are routed.
 */
public interface LiveCluster<R extends OutboundRequest,
        O extends OutboundResponse,
        E extends Endpoint> extends StickySessionFactory, OutboundThrower<R, E> {

    /**
     * Routes the given request to a list of suitable endpoints.
     *
     * @param request The outbound request to be routed.
     * @return A list of endpoints that can potentially handle the request.
     */
    CompletionStage<List<E>> route(R request);

    /**
     * Retrieves the default {@link ClusterPolicy} for the given request.
     *
     * @param request The request for which the default {@link ClusterPolicy} is to be obtained.
     * @return The default {@link ClusterPolicy} for the specified request, or {@code null} if there is none.
     */
    default ClusterPolicy getDefaultPolicy(R request) {
        return null;
    }

    /**
     * Executes an outbound invocation synchronously and returns the result.
     *
     * @param invocation The outbound invocation details, including the request data. This is used
     *                   to initiate the asynchronous operation.
     * @return The result of the outbound invocation. If the operation completes exceptionally, a
     * response representing the error condition is returned.
     */
    default O request(OutboundInvocation<R> invocation) {
        return request(invocation, null);
    }

    /**
     * Executes an outbound invocation synchronously and returns the result.
     *
     * @param invocation The outbound invocation details, including the request data. This is used
     *                   to initiate the asynchronous operation.
     * @param instances  A list of instances (e.g., service instances, client proxies) involved in
     *                   the invocation. These instances are used by the asynchronous operation.
     * @return The result of the outbound invocation. If the operation completes exceptionally, a
     * response representing the error condition is returned.
     */
    default O request(OutboundInvocation<R> invocation, List<E> instances) {
        try {
            // TODO timeout
            return invoke(invocation, instances).toCompletableFuture().get();
        } catch (InterruptedException e) {
            return createResponse(e, invocation.getRequest(), null);
        } catch (Throwable e) {
            return createResponse(getCause(e), invocation.getRequest(), null);
        }
    }

    /**
     * Executes a service request against a live cluster of endpoints.
     *
     * @param invocation The {@link OutboundInvocation} defining the outbound invocation logic.
     *                   This parameter specifies how the request should be executed, including
     *                   the selection of endpoints, serialization of the request, and handling
     *                   of responses.
     * @param instances  A list of {@code E} instances representing the available endpoints or
     *                   services against which the request can be executed. This list is typically
     *                   determined based on the current state of the cluster and the applicable
     *                   routing policies.
     * @return An outbound response of type {@code O}, corresponding to the executed request.
     * The response type is generic and can be adapted based on the specific needs of
     * the implementation.
     */
    default CompletionStage<O> invoke(OutboundInvocation<R> invocation, List<E> instances) {
        if (instances != null && !instances.isEmpty()) {
            invocation.setInstances(instances);
        }
        return invoke(invocation);
    }

    /**
     * Executes a service request against a live cluster of endpoints. The method handles
     * the entire invocation process, including selecting endpoints based on the provided
     * routing function, invoking the request on the selected endpoints, and returning the
     * corresponding response.
     *
     * @param invocation The outbound invocation logic that defines how the request should be executed.
     * @return An outbound response of type {@code O} that corresponds to the executed request.
     */
    default CompletionStage<O> invoke(OutboundInvocation<R> invocation) {
        ClusterPolicy defaultPolicy = getDefaultPolicy(invocation.getRequest());
        ClusterInvoker invoker = invocation.getContext().getClusterInvoker(invocation, defaultPolicy);
        return invoker.execute(this, invocation, defaultPolicy);
    }

    /**
     * Executes a service request with result transformation.
     * Handles endpoint selection, request execution, error checking and result conversion.
     *
     * @param invocation The outbound invocation to execute
     * @param function   Function to transform the response from type O to T
     * @param <T>        Target type after transformation
     * @return CompletionStage with transformed result or error
     */
    default <T> CompletionStage<T> invoke(OutboundInvocation<R> invocation, Function<O, T> function) {
        return invoke(invocation).thenCompose(r -> {
            // handle service error.
            ServiceError error = r == null ? null : r.getError();
            if (error != null && error.getThrowable() != null) {
                return Futures.future(error.getThrowable());
            } else {
                // handle conversion
                return CompletableFuture.completedFuture(function.apply(r));
            }
        });
    }

    /**
     * Executes a service request and transforms its result.
     *
     * @param invocation        The invocation to execute
     * @param valueFunction     Function to transform successful response
     * @param exceptionFunction Function to handle errors
     * @param <T>               Type of transformed result
     * @return CompletionStage containing either transformed result or error
     */
    default <T> CompletionStage<T> invoke(OutboundInvocation<R> invocation, Function<O, T> valueFunction, Function<Throwable, T> exceptionFunction) {
        return invoke(invocation).thenCompose(r -> {
            // handle service error.
            ServiceError error = r == null ? null : r.getError();
            if (error != null && error.getThrowable() != null) {
                return CompletableFuture.completedFuture(exceptionFunction.apply(error.getThrowable()));
            } else {
                // handle conversion
                return CompletableFuture.completedFuture(valueFunction.apply(r));
            }
        }).exceptionally(exceptionFunction);
    }

    /**
     * Invokes the given request on the specified endpoint.
     *
     * @param request  The request to be invoked.
     * @param endpoint The endpoint on which to invoke the request.
     * @return The response from the invocation.
     */
    CompletionStage<O> invoke(R request, E endpoint);

    /**
     * Creates a response object based on the provided throwable.
     *
     * @param throwable The exception that occurred during invocation.
     * @param request   The request.
     * @param endpoint  The endpoint.
     * @return A response object representing the error.
     */
    O createResponse(Throwable throwable, R request, E endpoint);

    /**
     * Returns the retry predicate used to determine if a failed operation should be retried.
     *
     * @return the retry predicate, or null if no retry predicate is set.
     */
    default ErrorPredicate getRetryPredicate() {
        return null;
    }

    /**
     * Checks if the current instance has been destroyed or marked for destruction.
     *
     * @return {@code true} if the instance has been destroyed or is otherwise marked as no longer valid, {@code false}
     * otherwise.
     */
    default boolean isDestroyed() {
        return false;
    }

    /**
     * Called when a request starts. This method provides a hook that can be used to perform actions
     * before the actual processing of the request begins.
     *
     * @param request the request object that is about to be processed
     */
    default void onStart(R request) {

    }

    /**
     * A default method that is called when a request is discarded.
     *
     * @param request the request that was discarded
     */
    default void onDiscard(R request) {

    }

    /**
     * Handles retry logic for a process or operation, providing a hook for custom actions on each retry attempt.
     *
     * @param request The request object that is about to be sent
     * @param retries The number of retry attempts that have occurred so far. This count is 1-based, meaning that
     *                the first retry attempt will pass a value of 1.
     */
    default void onRetry(R request, int retries) {

    }

    /**
     * Completes the future with response or exception after retry operations.
     *
     * @param future   the CompletableFuture to complete
     * @param request  the original request
     * @param response the response, may be null if exception occurred
     * @param e        the exception, may be null if successful
     */
    default void onRetryComplete(CompletableFuture<O> future, R request, O response, Throwable e) {
        if (e != null) {
            future.completeExceptionally(e);
        } else {
            future.complete(response);
        }
    }

    /**
     * Called when a request is about to be sent to a specific endpoint. This method can be used to
     * perform actions or modifications to the request specific to the endpoint it is being sent to.
     *
     * @param request  the request object that is about to be sent
     * @param endpoint the endpoint to which the request will be sent
     */
    default void onStartRequest(R request, E endpoint) {

    }

    /**
     * Called when an error occurs during the processing of a request to an endpoint. This method
     * can be used to handle errors, log them, or perform any cleanup if necessary.
     *
     * @param request   the request object that encountered an error
     * @param throwable the request object that encountered an error
     * @param endpoint  the endpoint at which the error occurred
     */
    default void onError(Throwable throwable, R request, E endpoint) {

    }

    /**
     * Called when a request is processed successfully and a response is received from an endpoint.
     * This method can be used to perform actions based on the successful response, such as logging,
     * statistics collection, or any post-response processing.
     *
     * @param response the response object received from the endpoint after successful processing
     * @param request  the request object that was successfully processed
     * @param endpoint the endpoint that successfully processed the request and provided a response
     */
    default void onSuccess(O response, R request, E endpoint) {

    }

    /**
     * Handles the recovery of a request by delegating to the onSuccess method.
     *
     * @param response The response object.
     * @param request The request object that was recovered.
     * @param endpoint The endpoint associated with the request.
     */
    default void onRecover(O response, R request, E endpoint) {
        onSuccess(response, request, endpoint);
    }

}

