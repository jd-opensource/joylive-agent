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
package com.jd.live.agent.plugin.router.sofarpc.response;

import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.jd.live.agent.governance.response.AbstractRpcResponse.AbstractRpcOutboundResponse;
import com.jd.live.agent.governance.response.Response;

import java.util.function.Predicate;

/**
 * Represents a response in the SOFA RPC framework.
 * <p>
 * The SOFA RPC framework is designed to provide a high-performance, scalable, and extensible RPC mechanism
 * suitable for microservices architecture. This interface is used to represent the responses that are
 * exchanged within the SOFA RPC framework, allowing for a standardized way of handling RPC responses.
 * </p>
 *
 * @since 1.0.0
 */
public interface SofaRpcResponse {

    /**
     * A concrete implementation of {@link SofaRpcResponse} that encapsulates the outbound response
     * of a SOFA RPC call. This class extends {@code AbstractRpcOutboundResponse<SofaResponse>} to
     * provide specific handling for SOFA RPC responses, including success and error states.
     */
    class SofaRpcOutboundResponse extends AbstractRpcOutboundResponse<SofaResponse> implements SofaRpcResponse {

        /**
         * Constructs a new {@code SofaRpcOutboundResponse} for a successful SOFA RPC call.
         * <p>
         * This constructor is used when the SOFA RPC call completes successfully, and a {@link SofaResponse}
         * is available. The response object contains the data returned by the RPC call.
         * </p>
         *
         * @param response The {@link SofaResponse} object containing the data returned by the successful RPC call.
         */
        public SofaRpcOutboundResponse(SofaResponse response) {
            super(response, null, null);
        }

        /**
         * Constructs a new {@code SofaRpcOutboundResponse} for a failed SOFA RPC call.
         * <p>
         * This constructor is used when the SOFA RPC call fails, and an exception is thrown.
         * The throwable represents the error that occurred during the RPC call. An optional
         * predicate can be provided to determine if the response should be retried based on
         * the type of error.
         * </p>
         *
         * @param throwable The {@code Throwable} that represents the error occurred during the RPC call.
         * @param predicate An optional {@code Predicate<Response>} that can be used to evaluate
         *                  whether the call should be retried based on the response. Can be {@code null}
         *                  if retry logic is not applicable.
         */
        public SofaRpcOutboundResponse(Throwable throwable, Predicate<Response> predicate) {
            super(null, throwable, predicate);
        }
    }
}

