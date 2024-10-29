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

import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

/**
 * An interface for creating cluster-specific exceptions based on the given Throwable object and request or endpoint.
 *
 * @param <R> The type of outbound request.
 * @param <E> The type of endpoint.
 */
public interface OutboundThrower<R extends OutboundRequest, E extends Endpoint> {

    /**
     * Creates and returns an exception based on the given Throwable object and request.
     *
     * @param throwable The Throwable object that caused the exception.
     * @param request   The request that triggered the exception.
     * @return An exception instance.
     */
    Throwable createException(Throwable throwable, R request);

    /**
     * Creates an exception based on the provided throwable.
     *
     * @param throwable The exception that occurred during invocation.
     * @param request   The request.
     * @param endpoint  The endpoint.
     * @return A response object representing the error.
     */
    Throwable createException(Throwable throwable, R request, E endpoint);

    /**
     * Creates and returns an exception based on the given Throwable object and invocation.
     *
     * @param throwable  The Throwable object that caused the exception.
     * @param invocation The OutboundInvocation that triggered the exception.
     * @return An exception instance.
     */
    Throwable createException(Throwable throwable, OutboundInvocation<R> invocation);
}
