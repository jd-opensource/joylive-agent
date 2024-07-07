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

import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.governance.exception.RetryException.RetryExhaustedException;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.request.StickyRequest;
import com.jd.live.agent.governance.response.Response;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

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
 * @param <T> The type of the exception that can be thrown during invocation.
 */
public interface LiveCluster<R extends OutboundRequest,
        O extends OutboundResponse,
        E extends Endpoint,
        T extends Throwable> extends StickyRequest {

    /**
     * Routes the given request to a list of suitable endpoints.
     *
     * @param request The outbound request to be routed.
     * @return A list of endpoints that can potentially handle the request.
     */
    CompletionStage<List<E>> route(R request);

    /**
     * Retrieves the default {@link ClusterPolicy} for the given request.
     * <p>
     * This method serves as a hook for providing a default policy when no specific policy is set
     * for an invocation. Implementing classes can override this method to supply custom logic
     * for determining the default policy based on the request or other contextual information.
     * </p>
     *
     * @param request The request for which the default {@link ClusterPolicy} is to be obtained.
     * @return The default {@link ClusterPolicy} for the specified request, or {@code null} if there is none.
     */
    // TODO change to ServicePolicy
    default ClusterPolicy getDefaultPolicy(R request) {
        return null;
    }

    /**
     * Executes a service request against a live cluster of endpoints.
     *
     * @param context    The {@link InvocationContext} providing additional information and state
     *                   necessary for the current invocation process. This includes metadata,
     *                   configuration settings, and potentially references to other relevant
     *                   components within the system.
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
    default CompletionStage<O> invoke(InvocationContext context, OutboundInvocation<R> invocation, List<E> instances) {
        if (instances != null && !instances.isEmpty()) {
            invocation.setInstances(instances);
        }
        return invoke(context, invocation);
    }

    /**
     * Executes an outbound invocation synchronously and returns the result.
     *
     * @param context    The invocation context, providing metadata or state relevant to the current
     *                   invocation. This context is passed to the asynchronous operation.
     * @param invocation The outbound invocation details, including the request data. This is used
     *                   to initiate the asynchronous operation.
     * @return The result of the outbound invocation. If the operation completes exceptionally, a
     * response representing the error condition is returned.
     */
    default O request(InvocationContext context, OutboundInvocation<R> invocation) {
        return request(context, invocation, null);
    }

    /**
     * Executes an outbound invocation synchronously and returns the result.
     *
     * @param context    The invocation context, providing metadata or state relevant to the current
     *                   invocation. This context is passed to the asynchronous operation.
     * @param invocation The outbound invocation details, including the request data. This is used
     *                   to initiate the asynchronous operation.
     * @param instances  A list of instances (e.g., service instances, client proxies) involved in
     *                   the invocation. These instances are used by the asynchronous operation.
     * @return The result of the outbound invocation. If the operation completes exceptionally, a
     * response representing the error condition is returned.
     */
    default O request(InvocationContext context, OutboundInvocation<R> invocation, List<E> instances) {
        try {
            // TODO timeout
            return invoke(context, invocation, instances).toCompletableFuture().get();
        } catch (InterruptedException e) {
            return createResponse(e, invocation.getRequest(), null);
        } catch (ExecutionException e) {
            return createResponse(e.getCause() != null ? e.getCause() : e, invocation.getRequest(), null);
        }
    }

    /**
     * Executes a service request against a live cluster of endpoints. The method handles
     * the entire invocation process, including selecting endpoints based on the provided
     * routing function, invoking the request on the selected endpoints, and returning the
     * corresponding response.
     *
     * @param context    The invocation context that provides additional information and state for
     *                   the current invocation process.
     * @param invocation The outbound invocation logic that defines how the request should be executed.
     * @return An outbound response of type {@code O} that corresponds to the executed request.
     */
    default CompletionStage<O> invoke(InvocationContext context, OutboundInvocation<R> invocation) {
        ClusterPolicy defaultPolicy = getDefaultPolicy(invocation.getRequest());
        ClusterInvoker invoker = context.getClusterInvoker(invocation, defaultPolicy);
        return invoker.execute(this, context, invocation, defaultPolicy);
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
     * Determines whether a given response indicates that the request is retryable.
     *
     * @param response The response to evaluate.
     * @return {@code true} if the request that generated the response should be retried; {@code false} otherwise.
     */
    default boolean isRetryable(Response response) {
        return false;
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
     * Creates and returns an exception indicating that cluster is not ready.
     *
     * @return An exception instance indicating that cluster is not ready.
     */
    default T createUnReadyException(R request) {
        return null;
    }

    /**
     * Creates and returns an exception indicating that the cluster is not ready.
     *
     * @param message The message explaining why the cluster is not ready.
     * @param request The request that cannot be processed because the cluster is not ready.
     * @return An instance of the exception indicating that the cluster is not ready.
     */
    default T createUnReadyException(String message, R request) {
        return null;
    }

    /**
     * Creates an exception based on the provided throwable.
     *
     * @param throwable The exception that occurred during invocation.
     * @param request   The request.
     * @param endpoint  The endpoint.
     * @return A response object representing the error.
     */
    T createException(Throwable throwable, R request, E endpoint);

    /**
     * Creates an exception to be thrown when no provider is available for the requested service.
     *
     * @param request The request for which no provider could be found.
     * @return An exception of type T indicating that no provider is available.
     */
    T createNoProviderException(R request);

    /**
     * Creates an exception to be thrown when a limit is reached for the requested service.
     *
     * @param exception The {@link RejectException} that caused the limit to be reached.
     * @param request   The request for which the limit has been reached.
     * @return An exception of type T indicating that a limit has been reached.
     */
    T createLimitException(RejectException exception, R request);

    /**
     * Creates an exception to be thrown when a circuit breaker is triggered for the requested service.
     *
     * @param exception The {@link RejectException} that caused the circuit breaker to be triggered.
     * @param request   The request for which the circuit breaker has been triggered.
     * @return An exception of type T indicating that a circuit breaker has been triggered.
     */
    T createCircuitBreakException(RejectException exception, R request);

    /**
     * Creates an exception to be thrown when a request is explicitly rejected.
     *
     * @param exception The original rejection exception.
     * @param request   The request for which no provider could be found.
     * @return An exception of type T representing the rejection.
     */
    T createRejectException(RejectException exception, R request);

    /**
     * Creates a new instance of a retry exhaustion exception.
     * <p>
     * This method is responsible for creating an exception object that signifies that the retry attempts have been exhausted.
     * The created exception typically encapsulates details from the original {@code RetryExhaustedException}, such as the
     * number of attempts made and the policy that governed the retries. This can be used to inform callers that no more
     * retries will be attempted and that the operation has ultimately failed after all allowed attempts.
     * </p>
     *
     * @param exception The original {@code RetryExhaustedException} that contains information about the exhausted retry attempts.
     * @return An instance of type {@code T} which extends {@code RetryExhaustedException}, with additional context or details if necessary.
     */
    T createRetryExhaustedException(RetryExhaustedException exception, OutboundInvocation<R> invocation);

    /**
     * Called when a request starts. This method provides a hook that can be used to perform actions
     * before the actual processing of the request begins.
     *
     * @param request the request object that is about to be processed
     */
    default void onStart(R request) {

    }

    /**
     * Handles retry logic for a process or operation, providing a hook for custom actions on each retry attempt.
     *
     * @param retries The number of retry attempts that have occurred so far. This count is 1-based, meaning that
     *                the first retry attempt will pass a value of 1.
     */
    default void onRetry(int retries) {

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

}

