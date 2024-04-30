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
package com.jd.live.agent.core.plugin.definition;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.plugin.definition.Interceptor;

/**
 * A base adapter class for {@link Interceptor} that provides empty implementations
 * for all methods. This class can be extended by concrete interceptors that only
 * need to implement a subset of the interceptor methods, avoiding the need to
 * implement all interface methods.
 * <p>
 * This adapter simplifies the creation of interceptors by allowing developers to
 * override only the methods they are interested in, providing default no-op
 * (no operation) implementations for the rest.
 */
public class InterceptorAdaptor implements Interceptor {

    /**
     * Enhanced logic before method execution. This method is called before the
     * target method is executed.
     * <p>
     * The default implementation does nothing.
     *
     * @param ctx The execution context of the method being intercepted.
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        // Default implementation does nothing
    }

    /**
     * Enhanced logic after method successfully executes. This method is called
     * after the target method completes successfully without throwing any exceptions.
     * <p>
     * The default implementation does nothing.
     *
     * @param ctx The execution context of the method being intercepted.
     */
    @Override
    public void onSuccess(ExecutableContext ctx) {
        // Default implementation does nothing
    }

    /**
     * Enhancement logic when method execution fails. This method is called if the
     * target method throws an exception during its execution.
     * <p>
     * The default implementation does nothing.
     *
     * @param ctx The execution context of the method being intercepted.
     */
    @Override
    public void onError(ExecutableContext ctx) {
        // Default implementation does nothing
    }

    /**
     * Enhanced logic after method execution. This method is called after the target
     * method completes its execution, whether it completes successfully or fails
     * due to an exception.
     * <p>
     * The default implementation does nothing.
     *
     * @param ctx The execution context of the method being intercepted.
     */
    @Override
    public void onExit(ExecutableContext ctx) {
        // Default implementation does nothing
    }
}

