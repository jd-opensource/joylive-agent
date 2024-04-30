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
package com.jd.live.agent.bootstrap.bytekit.advice;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.plugin.definition.Interceptor;

/**
 * The ExceptionHandler interface defines a contract for handling exceptions.
 * Implementations of this interface can provide custom exception handling logic.
 */
public interface ExceptionHandler {

    /**
     * Handles the given exception within the specified context and interceptor.
     *
     * @param context     The executable context within which the exception occurred.
     * @param interceptor The interceptor that was in use at the time of the exception.
     * @param throwable   The actual exception that occurred.
     * @param <T>         A generic type that extends ExecutableContext, representing the executable context.
     */
    <T extends ExecutableContext> void handle(T context, Interceptor interceptor, Throwable throwable);

    /**
     * A default, no-operation implementation of the ExceptionHandler interface.
     * This instance can be used as a placeholder or fallback when no specific exception handling is required.
     */
    ExceptionHandler EMPTY_EXCEPTION_HANDLER = new ExceptionHandler() {
        @Override
        public <T extends ExecutableContext> void handle(T context, Interceptor interceptor, Throwable throwable) {
        }
    };
}