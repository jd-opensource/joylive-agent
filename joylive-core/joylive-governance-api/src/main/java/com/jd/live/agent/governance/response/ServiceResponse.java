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

/**
 * ServiceResponse
 *
 * @since 1.0.0
 */
public interface ServiceResponse extends Response {

    /**
     * Retrieves the status code associated with this response. The status code
     * typically represents the outcome of the operation, such as success, failure,
     * or various error conditions.
     *
     * @return A {@code String} representing the status code of the response.
     */
    default String getCode() {
        return null;
    }

    /**
     * Retrieves any exception associated with an abnormal response. If the operation
     * resulted in an exception, this method provides access to the underlying issue.
     *
     * @return A {@code Throwable} representing the exception associated with the response,
     * or {@code null} if the operation completed without exceptions.
     */
    default ServiceError getError() {
        return null;
    }

    /**
     * Checks if the given throwable indicates a retryable error.
     *
     * @param throwable The throwable to check for retryability.
     * @return true if the error is retryable, false otherwise.
     */
    default boolean isRetryable(Throwable throwable) {
        return false;
    }

    /**
     * Checks if the service call was successful.
     *
     * @return true if the service call was successful, false otherwise.
     */
    default boolean isSuccess() {
        return getError() == null;
    }

    /**
     * Returns the real result of the service call.
     *
     * @return The real result of the service call, or null if no result is available.
     */
    default Object getResult() {
        return null;
    }

    /**
     * Defines an interface for outbound service response.
     * <p>
     * This interface represents the response received from another service or component from the current service。
     * </p>
     *
     * @since 1.0.0
     */
    interface OutboundResponse extends ServiceResponse {


    }
}
