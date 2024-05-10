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
package com.jd.live.agent.governance.invoke.retry;

import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.exception.RetryException;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.response.Response;

import java.util.function.Supplier;

/**
 * Defines the contract for implementing retry mechanisms.
 * <p>
 * A {@code Retrier} is responsible for executing operations that may fail transiently and are suitable for retrying.
 * It abstracts the retry logic, allowing operations to be attempted multiple times according to a specified
 * {@link RetryPolicy}. This interface is useful in scenarios where operations, such as network requests or
 * database transactions, are prone to failure due to temporary issues that can be resolved by retrying the operation
 * after a short delay or under different conditions.
 * </p>
 *
 * @since 1.0.0
 */
public interface Retrier {

    /**
     * Marker used to identify retry operations in logs or monitoring.
     */
    String RETRY_MARK = "retryMark";

    /**
     * Executes the given operation with retry logic.
     * <p>
     * This method attempts to execute the supplied operation, retrying according to the {@link RetryPolicy}
     * defined by {@link #getPolicy()}. It is designed to handle operations that return a result and may throw
     * a {@link RetryException} to indicate a failure that is potentially recoverable through retrying.
     * </p>
     *
     * @param supplier The operation to be executed, encapsulated as a {@link Supplier} that returns a response of type {@code T}.
     * @param <T>      The type of response expected from the operation, extending {@link Response}.
     * @return The response of type {@code T} from the successfully executed operation.
     * @throws RetryException if the operation fails to complete successfully after the maximum number of retries.
     */
    <T extends Response> T execute(Supplier<T> supplier) throws RetryException;

    /**
     * Retrieves the retry policy governing the behavior of this retrier.
     * <p>
     * The {@link RetryPolicy} determines the conditions under which an operation should be retried, including
     * the maximum number of retry attempts, the delay between attempts, and any other criteria relevant to
     * deciding whether and how to retry a failed operation.
     * </p>
     *
     * @return The {@link RetryPolicy} associated with this retrier.
     */
    RetryPolicy getPolicy();

    default Throwable getCause(Throwable throwable) {
        Throwable result = RequestContext.getAttribute(Response.KEY_LAST_EXCEPTION);
        if (result != null) {
            return result;
        } else if (throwable.getCause() != null) {
            return throwable.getCause();
        } else {
            return throwable;
        }
    }
}

