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
package com.jd.live.agent.plugin.router.dubbo.v2_6.response;

import com.alibaba.dubbo.rpc.Result;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.response.AbstractRpcResponse.AbstractRpcOutboundResponse;

/**
 * Represents a response in the Dubbo RPC system. This interface serves as a marker
 * for responses that are specific to Dubbo's communication mechanism.
 *
 * @since 1.0.0
 */
public interface DubboResponse {

    /**
     * A concrete implementation of {@link DubboResponse} that encapsulates the result of
     * an RPC call within Dubbo's system. It extends {@link AbstractRpcOutboundResponse} to
     * provide additional context and handling for Dubbo-specific responses.
     */
    class DubboOutboundResponse extends AbstractRpcOutboundResponse<Result> implements DubboResponse {

        /**
         * Constructs a new {@link DubboOutboundResponse} with the given result.
         * This constructor is used when the RPC call has completed successfully.
         *
         * @param response The result of the RPC call.
         */
        public DubboOutboundResponse(Result response) {
            this(response, null);
        }

        /**
         * Constructs a new {@link DubboOutboundResponse} with the given error.
         * This constructor is used when the RPC call resulted in an error.
         *
         * @param error          The error that occurred during the RPC call.
         * @param retryPredicate A predicate to test the response and determine if it is retryable.
         */
        public DubboOutboundResponse(ServiceError error, ErrorPredicate retryPredicate) {
            super(null, error, retryPredicate);
        }

        /**
         * Constructs a new {@link DubboOutboundResponse} with both a result and a throwable.
         * This constructor can be used when the RPC call has a result but also encountered
         * an exception that might be recoverable or informative.
         *
         * @param response       The result of the RPC call.
         * @param retryPredicate A predicate to test the response and determine if it is retryable.
         */
        public DubboOutboundResponse(Result response, ErrorPredicate retryPredicate) {
            super(response,
                    response != null && response.hasException()
                            ? new ServiceError(response.getException(), true)
                            : null,
                    retryPredicate);
        }

    }
}

