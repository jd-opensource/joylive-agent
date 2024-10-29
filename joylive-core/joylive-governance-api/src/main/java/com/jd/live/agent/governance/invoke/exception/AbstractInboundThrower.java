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

import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.*;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;

/**
 * An abstract implementation of the InboundThrower interface.
 *
 * @param <R> The type of outbound request.
 */
public abstract class AbstractInboundThrower<R extends InboundRequest> implements InboundThrower<R> {

    @Override
    public Throwable createException(Throwable throwable, R request) {
        if (throwable == null) {
            return null;
        } else if (throwable instanceof RejectUnreadyException) {
            return createUnReadyException((RejectUnreadyException) throwable, request);
        } else if (throwable instanceof RejectAuthException) {
            return createAuthException((RejectAuthException) throwable, request);
        } else if (throwable instanceof RejectPermissionException) {
            return createPermissionException((RejectPermissionException) throwable, request);
        } else if (throwable instanceof RejectEscapeException) {
            return createEscapeException((RejectEscapeException) throwable, request);
        } else if (throwable instanceof RejectLimitException) {
            return createLimitException((RejectLimitException) throwable, request);
        } else if (throwable instanceof RejectCircuitBreakException) {
            return createCircuitBreakException((RejectCircuitBreakException) throwable, request);
        } else if (throwable instanceof RejectException) {
            return createRejectException((RejectException) throwable, request);
        } else if (throwable instanceof LiveException) {
            return createLiveException((LiveException) throwable, request);
        } else {
            return throwable;
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
     * Creates an unknown exception response for the given Throwable object, request, and endpoint.
     *
     * @param exception The Throwable object that caused the exception.
     * @param request   The request that triggered the exception.
     * @return The created unknown exception response.
     */
    protected abstract Throwable createLiveException(LiveException exception, R request);

    /**
     * Creates an exception to be thrown when failed authenticate the requested service.
     *
     * @param exception The {@link RejectAuthException} that caused the limit to be reached.
     * @param request   The request for which no provider could be found.
     * @return An exception of type T indicating that no provider is available.
     */
    protected abstract Throwable createAuthException(RejectAuthException exception, R request);

    /**
     * Creates an exception to be thrown when no permission for the requested service.
     *
     * @param exception The {@link RejectPermissionException} that caused the limit to be reached.
     * @param request   The request for which no provider could be found.
     * @return An exception of type T indicating that no provider is available.
     */
    protected abstract Throwable createPermissionException(RejectPermissionException exception, R request);

    /**
     * Creates an exception to be thrown when a limit is reached for the requested service.
     *
     * @param exception The {@link RejectLimitException} that caused the limit to be reached.
     * @param request   The request for which the limit has been reached.
     * @return An exception of type T indicating that a limit has been reached.
     */
    protected abstract Throwable createLimitException(RejectLimitException exception, R request);

    /**
     * Creates an exception to be thrown when a circuit breaker is triggered for the requested service.
     *
     * @param exception The {@link RejectCircuitBreakException} that caused the circuit breaker to be triggered.
     * @param request   The request for which the circuit breaker has been triggered.
     * @return An exception of type T indicating that a circuit breaker has been triggered.
     */
    protected abstract Throwable createCircuitBreakException(RejectCircuitBreakException exception, R request);

    /**
     * Creates an exception to be thrown for the escaped requested.
     *
     * @param exception The {@link RejectPermissionException} that caused the limit to be reached.
     * @param request   The request for which no provider could be found.
     * @return An exception of type T indicating that no provider is available.
     */
    protected abstract Throwable createEscapeException(RejectEscapeException exception, R request);

    /**
     * Creates an exception to be thrown when a request is explicitly rejected.
     *
     * @param exception The original rejection exception.
     * @param request   The request for which no provider could be found.
     * @return An exception of type T representing the rejection.
     */
    protected abstract Throwable createRejectException(RejectException exception, R request);

}
