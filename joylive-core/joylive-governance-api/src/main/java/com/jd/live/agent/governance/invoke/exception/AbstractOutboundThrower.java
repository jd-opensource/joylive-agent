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
package com.jd.live.agent.governance.invoke.exception;

import com.jd.live.agent.bootstrap.exception.FaultException;
import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectNoProviderException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectUnreadyException;
import com.jd.live.agent.governance.exception.RetryException.RetryExhaustedException;
import com.jd.live.agent.governance.exception.RetryException.RetryTimeoutException;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

/**
 * An abstract implementation of the OutboundThrower interface.
 *
 * @param <R> The type of outbound request.
 * @param <E> The type of endpoint.
 */
public abstract class AbstractOutboundThrower<
        R extends OutboundRequest,
        E extends Endpoint> implements OutboundThrower<R, E> {

    @Override
    public Throwable createException(Throwable throwable, R request) {
        return createException(throwable, request, null);
    }

    @Override
    public Throwable createException(Throwable throwable, R request, E endpoint) {
        if (throwable == null) {
            return null;
        } else if (throwable instanceof RejectUnreadyException) {
            return createUnReadyException((RejectUnreadyException) throwable, request);
        } else if (throwable instanceof RejectNoProviderException) {
            return createNoProviderException((RejectNoProviderException) throwable, request);
        } else if (throwable instanceof RejectCircuitBreakException) {
            return createCircuitBreakException((RejectCircuitBreakException) throwable, request);
        } else if (throwable instanceof RejectException) {
            return createRejectException((RejectException) throwable, request);
        } else if (throwable instanceof FaultException) {
            return createFaultException((FaultException) throwable, request);
        } else if (throwable instanceof LiveException) {
            return createLiveException((LiveException) throwable, request, endpoint);
        } else {
            return throwable;
        }
    }

    @Override
    public Throwable createException(Throwable throwable, OutboundInvocation<R> invocation) {
        if (throwable instanceof RetryExhaustedException) {
            return createRetryExhaustedException((RetryExhaustedException) throwable, invocation);
        } else if (throwable instanceof RetryTimeoutException) {
            return createRetryTimeoutException((RetryTimeoutException) throwable, invocation);
        } else {
            return createException(throwable, invocation.getRequest());
        }
    }

    /**
     * Creates and returns an exception indicating that the cluster is not ready.
     *
     * @param exception The original exception that caused the cluster to be unavailable.
     * @param request   The request for which no provider could be found due to the cluster being unavailable.
     * @return An exception instance indicating that the cluster is not ready.
     */
    protected abstract Throwable createUnReadyException(RejectUnreadyException exception, R request);

    /**
     * Creates a fault exception response for the given Throwable object, request, and endpoint.
     *
     * @param throwable The Throwable object that caused the fault.
     * @param request   The request that triggered the fault.
     * @return The created fault exception response.
     */
    protected abstract Throwable createFaultException(FaultException throwable, R request);

    /**
     * Creates an exception to be thrown when a circuit breaker is triggered for the requested service.
     *
     * @param exception The {@link RejectCircuitBreakException} that caused the circuit breaker to be triggered.
     * @param request   The request for which the circuit breaker has been triggered.
     * @return An exception of type Throwable indicating that a circuit breaker has been triggered.
     */
    protected abstract Throwable createCircuitBreakException(RejectCircuitBreakException exception, R request);

    /**
     * Creates an exception to be thrown when no provider is available for the requested service.
     *
     * @param exception The {@link RejectNoProviderException} that caused the limit to be reached.
     * @param request   The request for which no provider could be found.
     * @return An exception of type Throwable indicating that no provider is available.
     */
    protected abstract Throwable createNoProviderException(RejectNoProviderException exception, R request);

    /**
     * Creates an exception to be thrown when a request is explicitly rejected.
     *
     * @param exception The original rejection exception.
     * @param request   The request for which no provider could be found.
     * @return An exception of type Throwable representing the rejection.
     */
    protected abstract Throwable createRejectException(RejectException exception, R request);

    /**
     * Creates a new instance of a retry exhaustion exception.
     *
     * @param exception The original {@code RetryExhaustedException} that contains information about the exhausted retry attempts.
     * @return An exception of type Throwable representing retry exhaustion.
     */
    protected abstract Throwable createRetryExhaustedException(RetryExhaustedException exception, OutboundInvocation<R> invocation);

    /**
     * Creates a new instance of a retry timeout exception.
     *
     * @param exception The original {@code RetryTimeoutException} that contains information about the retry timeout.
     * @return An exception of type Throwable representing retry timeout.
     */
    protected abstract Throwable createRetryTimeoutException(RetryTimeoutException exception, OutboundInvocation<R> invocation);

    /**
     * Creates an unknown exception response for the given Throwable object, request, and endpoint.
     *
     * @param exception The Throwable object that caused the exception.
     * @param request   The request that triggered the exception.
     * @param endpoint  The endpoint where the exception occurred.
     * @return The created unknown exception response.
     */
    protected abstract Throwable createLiveException(LiveException exception, R request, E endpoint);

    /**
     * Constructs a detailed error message for a given throwable and RPC call context.
     *
     * @param throwable The {@code Throwable} that represents the error encountered.
     * @param request   The {@code DubboOutboundRequest} that contains details about the RPC request.
     * @param endpoint  The {@code DubboEndpoint} that contains details about the endpoint being called.
     * @return A {@code String} representing the detailed error message.
     */
    protected String getError(Throwable throwable, R request, E endpoint) {
        return throwable.getMessage();
    }
}
