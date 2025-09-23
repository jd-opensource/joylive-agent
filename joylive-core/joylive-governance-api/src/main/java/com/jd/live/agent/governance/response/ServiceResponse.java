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

import com.jd.live.agent.bootstrap.bytekit.context.ResultProvider;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.exception.ErrorParserPolicy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * ServiceResponse
 *
 * @since 1.0.0
 */
public interface ServiceResponse extends Response, ResultProvider {

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

    @Override
    default void handle(BiConsumer<Object, Throwable> consumer) {
        ServiceError error = getError();
        if (error != null && !error.isServerError()) {
            consumer.accept(null, error.getThrowable());
        } else {
            consumer.accept(getResponse(), null);
        }
    }

    /**
     * Gets the response or throws an exception if there's a non-server error.
     *
     * @return the response object
     * @throws Throwable if a non-server error occurred
     */
    default <T> T getResponseOrThrow() throws Throwable {
        return getResponseOrThrow(null);
    }

    /**
     * Gets the response or throws an exception if there's a non-server error.
     * Allows custom exception transformation via the provided thrower function.
     *
     * @param <T>     the type of the response
     * @param thrower function to transform the original exception, or null to use original
     * @return the response object cast to the specified type
     * @throws Throwable the original exception or transformed exception if a non-server error occurred
     */
    default <T, R extends Throwable> T getResponseOrThrow(BiFunction<String, Throwable, R> thrower) throws R {
        ServiceError error = getError();
        if (error != null && !error.isServerError()) {
            throw thrower == null ? (R) error.getThrowable() : thrower.apply(error.getError(), error.getThrowable());
        } else {
            return (T) getResponse();
        }
    }

    /**
     * Completes the given CompletableFuture based on the service error status.
     * Completes exceptionally if there's an error with a throwable, otherwise completes normally.
     *
     * @param future the CompletableFuture to complete
     */
    default void completeVoid(CompletableFuture<Void> future) {
        ServiceError error = getError();
        if (error != null && error.getThrowable() != null) {
            future.completeExceptionally(error.getThrowable());
        } else {
            future.complete(null);
        }
    }

    /**
     * Checks if the given error parser policy matches the criteria for this error handler.
     *
     * @param policy the error policy to check
     * @return true if the error policy matches, false otherwise (default implementation always returns false)
     */
    default boolean match(ErrorParserPolicy policy) {
        return false;
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

    /**
     * Marks a component as supporting asynchronous operations via {@link CompletionStage}.
     *
     * <p>The attached future completes when the asynchronous operation finishes,
     * either with a result or an exception.</p>
     */
    interface Asyncable {

        /**
         * Returns the future representing the asynchronous operation's outcome.
         *
         * @return a {@link CompletionStage} that completes when the operation finishes,
         * containing either the result or the failure exception
         */
        CompletionStage<Object> getFuture();
    }
}
