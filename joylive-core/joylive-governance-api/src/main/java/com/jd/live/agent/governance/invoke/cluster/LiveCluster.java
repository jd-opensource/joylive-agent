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
import com.jd.live.agent.governance.exception.RetryExhaustedException;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.request.StickyRequest;
import com.jd.live.agent.governance.response.Response;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.util.List;

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
    List<E> route(R request);

    /**
     * Checks if the specified endpoint is available to handle requests.
     * <p>
     * This method provides a default implementation, always returning {@code true},
     * indicating that by default, all endpoints are considered available.
     * Implementations may override this method to provide specific availability checks.
     * </p>
     *
     * @param endpoint The endpoint to check for availability.
     * @return {@code true} if the endpoint is available; {@code false} otherwise.
     */
    default boolean isAvailable(E endpoint) {
        return true;
    }

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
    default ClusterPolicy getDefaultPolicy(R request) {
        return null;
    }

    /**
     * Executes a service request against a live cluster of endpoints. This method orchestrates
     * the entire invocation process, encapsulating the logic for endpoint selection based on
     * the provided routing function, executing the request on the chosen endpoints, and
     * managing the response.
     * </p>
     *
     * @param context    The {@link InvocationContext} providing additional information and state
     *                   necessary for the current invocation process. This includes metadata,
     *                   configuration settings, and potentially references to other relevant
     *                   components within the system.
     * @param invocation The {@link OutboundInvocation}&lt;R&gt; defining the outbound invocation logic.
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
     * @throws T If an error occurs during the execution of the request. The specific type of
     *           exception {@code T} is defined by the type parameter, allowing for flexibility
     *           in error handling and enabling the method to throw exceptions that are meaningful
     *           within the context of the caller's domain.
     */
    default O invoke(InvocationContext context, OutboundInvocation<R> invocation, List<E> instances) throws T {
        if (instances != null && !instances.isEmpty()) {
            invocation.setInstances(instances);
        }
        return invoke(context, invocation);
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
     * @throws T If an error occurs during the execution of the request. The specific type of error
     *           is defined by the type parameter {@code T}.
     */
    default O invoke(InvocationContext context, OutboundInvocation<R> invocation) throws T {
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
     * @throws T If the invocation fails, an exception of type T is thrown.
     */
    O invoke(R request, E endpoint) throws T;

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
    boolean isRetryable(Response response);

    /**
     * Creates an exception to be thrown when no provider is available for the requested service.
     *
     * @param request The request for which no provider could be found.
     * @return An exception of type T indicating that no provider is available.
     */
    T createNoProviderException(R request);

    /**
     * Creates an exception to be thrown when a request is explicitly rejected.
     *
     * @param exception The original rejection exception.
     * @return An exception of type T representing the rejection.
     */
    T createRejectException(RejectException exception);

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

}

