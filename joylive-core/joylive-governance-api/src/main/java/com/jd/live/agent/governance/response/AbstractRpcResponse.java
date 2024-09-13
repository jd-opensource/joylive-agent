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
package com.jd.live.agent.governance.response;

import java.util.function.Predicate;

/**
 * Represents the abstract base for all RPC (Remote Procedure Call) responses.
 * Extends {@link AbstractServiceResponse} to include RPC-specific response handling
 * and potentially additional metadata pertinent to RPC operations.
 *
 * @param <T> the type of the response content specific to RPC operations
 * @since 1.0.0
 */
public abstract class AbstractRpcResponse<T> extends AbstractServiceResponse<T> implements RpcResponse {

    /**
     * Constructs an instance of {@code AbstractRpcResponse} with the specified
     * RPC response content and throwable. This constructor assumes no custom retry
     * predicate, applying the default retry logic.
     *
     * @param response  the RPC response content
     * @param throwable the throwable, if any, that occurred during the RPC operation
     */
    public AbstractRpcResponse(T response, ServiceError throwable) {
        super(response, throwable, null);
    }

    /**
     * Constructs an instance of {@code AbstractRpcResponse} with the specified
     * RPC response content, throwable, and a custom retry predicate.
     *
     * @param response  the RPC response content
     * @param throwable the throwable, if any, that occurred during the RPC operation
     * @param predicate a custom predicate to evaluate retryability of the RPC response
     */
    public AbstractRpcResponse(T response, ServiceError throwable, Predicate<Throwable> predicate) {
        super(response, throwable, predicate);
    }

    /**
     * AbstractRpcOutboundResponse is a nested abstract class within {@code AbstractRpcResponse}
     * that specifically represents responses for outbound RPC operations. This could include
     * additional features or behaviors that are unique to outbound communication in RPC services.
     *
     * @param <T> the type of the outbound RPC response content
     */
    public abstract static class AbstractRpcOutboundResponse<T> extends AbstractRpcResponse<T> implements RpcOutboundResponse {

        /**
         * Constructs an instance of {@code AbstractRpcOutboundResponse} with the specified
         * outbound RPC response content and throwable. This constructor defaults to no custom
         * retry predicate, using the inherited retry logic.
         *
         * @param response  the outbound RPC response content
         * @param throwable the throwable, if any, that occurred during the outbound RPC operation
         */
        public AbstractRpcOutboundResponse(T response, ServiceError throwable) {
            super(response, throwable);
        }

        /**
         * Constructs an instance of {@code AbstractRpcOutboundResponse} with the specified
         * outbound RPC response content, throwable, and a custom retry predicate.
         *
         * @param response  the outbound RPC response content
         * @param throwable the throwable, if any, that occurred during the outbound RPC operation
         * @param predicate a custom predicate to evaluate retryability of the outbound RPC response
         */
        public AbstractRpcOutboundResponse(T response, ServiceError throwable, Predicate<Throwable> predicate) {
            super(response, throwable, predicate);
        }
    }
}

