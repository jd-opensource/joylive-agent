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

import com.jd.live.agent.bootstrap.util.Attributes;

/**
 * Defines the structure for a response object in a system that processes operations
 * which can result in success or failure. The {@code Response} interface extends
 * {@code Attributes} to include additional details specific to the response such
 * as status codes, exceptions, and the original response data.
 *
 * @since 1.0.0
 */
public interface Response extends Attributes {

    String KEY_LAST_EXCEPTION = "lastException";

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
     *         or {@code null} if the operation completed without exceptions.
     */
    default Throwable getThrowable() {
        return null;
    }

    /**
     * Retrieves the original response object. This method provides access to the
     * complete response data as received from the operation.
     *
     * @return An {@code Object} representing the original response.
     */
    default Object getResponse() {
        return null;
    }

    /**
     * Determines whether the response should be considered for a retry. This default
     * implementation returns {@code false}, indicating that by default, responses are not
     * retried. Implementations can override this method to introduce custom logic for
     * determining retry eligibility based on the specifics of the response.
     *
     * @return {@code true} if the response meets conditions that warrant a retry, {@code false} otherwise.
     */
    default boolean isRetryable() {
        return false;
    }
}
