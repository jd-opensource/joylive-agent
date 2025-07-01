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
package com.jd.live.agent.plugin.failover.lettuce.v6.context;

import com.jd.live.agent.core.util.ExceptionUtils;
import com.jd.live.agent.core.util.Futures;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

/**
 * Thread-local context management for Lettuce operations.
 * Provides ignore flag control and async execution utilities.
 */
public class LettuceContext {

    // Ignore interceptor
    private static final ThreadLocal<Boolean> IGNORED = new ThreadLocal<>();

    /**
     * Checks if current thread operations should be ignored.
     *
     * @return true if operations should be ignored, false otherwise
     */
    public static boolean isIgnored() {
        Boolean result = IGNORED.get();
        return result != null && result;
    }

    /**
     * Marks current thread operations to be ignored.
     */
    public static void ignore() {
        IGNORED.set(Boolean.TRUE);
    }

    /**
     * Clears the ignore flag for current thread.
     */
    public static void remove() {
        IGNORED.remove();
    }

    /**
     * Executes async operation with temporary ignore flag.
     *
     * @param callable The async operation to execute
     * @param <T>      The result type of the async operation
     * @return CompletionStage representing the async result
     */
    public static <T> CompletionStage<T> callAsync(Callable<CompletionStage<T>> callable) {
        LettuceContext.ignore();
        try {
            return callable.call();
        } catch (Throwable e) {
            return Futures.future(ExceptionUtils.getCause(e));
        } finally {
            LettuceContext.remove();
        }
    }
}
