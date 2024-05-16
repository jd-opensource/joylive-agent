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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * This class provides utility methods for working with futures and completion stages.
 */
public abstract class Futures {

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

}

