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
package com.jd.live.agent.bootstrap.plugin.definition;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;

/**
 * Interceptor interface for providing custom logic at different stages of method execution.
 * It allows for interception before execution, after successful execution, upon error, and upon exit.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public interface Interceptor {

    /**
     * Invoked before the method execution.
     *
     * @param ctx the executable context
     */
    void onEnter(ExecutableContext ctx);

    /**
     * Invoked after the method has successfully executed.
     *
     * @param ctx the executable context
     */
    void onSuccess(ExecutableContext ctx);

    /**
     * Invoked when an error occurs during method execution.
     *
     * @param ctx the executable context
     */
    void onError(ExecutableContext ctx);

    /**
     * Invoked upon exiting the method execution.
     *
     * @param ctx the executable context
     */
    void onExit(ExecutableContext ctx);

}

