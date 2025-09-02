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
package com.jd.live.agent.core.util;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.BiFunction;

/**
 * This class provides utility methods for working with futures and completion stages.
 */
public abstract class Futures {

    /**
     * Completes a CompletableFuture with either an exception or a value.
     *
     * @param future    the CompletableFuture to complete
     * @param value     the value to complete with, or null if completing with an exception
     * @param throwable the exception to complete with, or null if completing with a value
     * @param <T>       the type of the value
     */
    public static <T> void complete(CompletableFuture<T> future, T value, Throwable throwable) {
        if (future != null) {
            if (throwable != null) {
                future.completeExceptionally(throwable);
            } else {
                future.complete(value);
            }
        }
    }

    /**
     * Converts a {@link CompletionStage} into a {@link CompletableFuture}.
     *
     * @param <T>   the result type returned by this future's {@code get} method
     * @param stage the {@link CompletionStage} to convert to {@link CompletableFuture}
     * @return a {@link CompletableFuture} representing the same completion stage, or
     * {@code null} if the input {@code stage} is {@code null}
     */
    public static <T> CompletableFuture<T> future(CompletionStage<T> stage) {
        return stage == null ? null : stage.toCompletableFuture();
    }

    /**
     * This method creates a CompletableFuture that is already completed exceptionally with the given Throwable.
     *
     * @param <T>       The type of the CompletableFuture's result
     * @param throwable The Throwable to be used for completing the CompletableFuture exceptionally
     * @return A CompletableFuture that is already completed exceptionally with the given Throwable
     */
    public static <T> CompletableFuture<T> future(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    public static <T> CompletableFuture<Void> allOf(List<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Synchronously gets the result from a CompletionStage with timeout and error handling.
     *
     * @param <T>           the result type
     * @param stage         the CompletionStage to await
     * @param timeout       the timeout value
     * @param unit          the timeout time unit
     * @param action        the action description for error messages
     * @param errorFunction function to handle and transform errors
     * @return the result from the CompletionStage
     * @throws Throwable if execution fails, times out, or is interrupted
     */
    public static <T> T get(final CompletionStage<T> stage,
                            final long timeout,
                            final TimeUnit unit,
                            final String action,
                            final BiFunction<String, Throwable, Throwable> errorFunction) throws Throwable {
        try {
            return stage.toCompletableFuture().get(timeout < 0 ? 0 : timeout, unit);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String errorMessage = "Failed to " + action + ", caused by " + cause.getMessage();
            throw errorFunction.apply(errorMessage, cause);
        } catch (TimeoutException e) {
            String errorMessage = "Failed to " + action + ", caused by it's timeout.";
            throw errorFunction.apply(errorMessage, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMessage = "Failed to " + action + ", caused by it's interrupted";
            throw errorFunction.apply(errorMessage, e);
        } catch (Throwable e) {
            String errorMessage = "Failed to " + action + ", caused by " + e.getMessage();
            throw errorFunction.apply(errorMessage, e);
        }
    }

    /**
     * Synchronously invokes a void CompletionStage with timeout and error handling.
     *
     * @param <T>           the error result type
     * @param stage         the void CompletionStage to await
     * @param timeout       the timeout value
     * @param unit          the timeout time unit
     * @param action        the action description for error messages
     * @param errorFunction function to handle and transform errors
     * @return null on success, or error result from errorFunction on failure
     */
    public static <T> T invoke(final CompletionStage<Void> stage,
                               final long timeout,
                               final TimeUnit unit,
                               final String action,
                               final BiFunction<String, Throwable, T> errorFunction) {
        try {
            stage.toCompletableFuture().get(timeout < 0 ? 0 : timeout, unit);
            return null;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String errorMessage = "Failed to " + action + ", caused by " + cause.getMessage();
            return errorFunction.apply(errorMessage, cause);
        } catch (TimeoutException e) {
            String errorMessage = "Failed to " + action + ", caused by it's timeout.";
            return errorFunction.apply(errorMessage, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMessage = "Failed to " + action + ", caused by it's interrupted";
            return errorFunction.apply(errorMessage, e);
        } catch (Throwable e) {
            String errorMessage = "Failed to " + action + ", caused by " + e.getMessage();
            return errorFunction.apply(errorMessage, e);
        }
    }

}

