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
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.response.AbstractRpcResponse.AbstractRpcOutboundResponse;

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
         *
         * @param response The {@link SofaResponse} object containing the data returned by the successful RPC call.
         */
        public SofaRpcOutboundResponse(SofaResponse response) {
            this(response, null);
        }

        /**
         * Constructs a new {@code SofaRpcOutboundResponse} for a successful SOFA RPC call.
         *
         * @param response  The {@link SofaResponse} object containing the data returned by the successful RPC call.
         * @param predicate An optional {@code Predicate<Response>} that can be used to evaluate
         *                  whether the call should be retried based on the response. Can be {@code null}
         *                  if retry logic is not applicable.
         */
        public SofaRpcOutboundResponse(SofaResponse response, ErrorPredicate predicate) {
            super(response, getError(response), predicate);
        }

        /**
         * Constructs a new {@code SofaRpcOutboundResponse} for a failed SOFA RPC call.
         *
         * @param error The {@code Throwable} that represents the error occurred during the RPC call.
         * @param predicate An optional {@code Predicate<Response>} that can be used to evaluate
         *                  whether the call should be retried based on the response. Can be {@code null}
         *                  if retry logic is not applicable.
         */
        public SofaRpcOutboundResponse(ServiceError error, ErrorPredicate predicate) {
            super(null, error, predicate);
        }

        /**
         * Extracts the error information from a SofaResponse object.
         *
         * @param response The SofaResponse object to extract the error information from.
         * @return A ServiceError object containing the error information, or null if no error is found.
         */
        private static ServiceError getError(SofaResponse response) {
            if (response == null) {
                return null;
            } else if (response.isError()) {
                return new ServiceError(response.getErrorMsg(), true);
            } else if (response.getAppResponse() instanceof Throwable) {
                return new ServiceError((Throwable) response.getAppResponse(), true);
            }
            return null;
        }

    }
}

