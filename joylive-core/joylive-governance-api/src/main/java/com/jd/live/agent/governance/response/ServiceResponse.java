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

import com.jd.live.agent.governance.exception.ErrorPolicy;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;

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
     * Returns the retry predicate used to determine if a failed operation should be retried.
     *
     * @return the retry predicate, or null if no retry predicate is set.
     */
    default ErrorPredicate getRetryPredicate() {
        return null;
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
     * Checks if the given error policy matches the criteria for this error handler.
     *
     * @param errorPolicy the error policy to check
     * @return true if the error policy matches, false otherwise (default implementation always returns false)
     */
    default boolean match(ErrorPolicy errorPolicy) {
        return false;
    }

    /**
     * Returns the exception message of the service call.
     *
     * @return exception message or null
     */
    default String getExceptionMessage() {
        return null;
    }

    /**
     * Returns the exception names of the service call.
     * @return exception names or null
     */
    default String getExceptionNames() {
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
