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
package com.jd.live.agent.plugin.router.springcloud.v2.exception;

import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpStatus;

/**
 * A factory interface for creating instances of {@link NestedRuntimeException}.
 * This interface is designed to provide a flexible way to create custom exceptions
 * based on the provided HTTP request, status, message, and throwable.
 *
 * @param <T> the type of the exception to be created, which must extend {@link NestedRuntimeException}
 * @param <R> the type of the HTTP outbound request, which must extend {@link HttpOutboundRequest}
 */
public interface ThrowerFactory<T extends Throwable, R extends HttpOutboundRequest> {

    /**
     * Creates an instance of {@link NestedRuntimeException} using the provided HTTP request, status, message, and throwable.
     *
     * @param request   the HTTP outbound request associated with the error
     * @param status    the HTTP status code of the error
     * @param message   the error message describing the issue
     * @param throwable the cause of the exception
     * @return an instance of {@link NestedRuntimeException} with the specified details
     */
    T createException(R request, HttpStatus status, String message, Throwable throwable);

}

