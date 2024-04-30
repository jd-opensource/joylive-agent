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
package com.jd.live.agent.bootstrap.bytekit.context;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A context representing the execution state of a method.
 */
public class MethodContext extends ExecutableContext {

    /**
     * The Method instance representing the method to be executed.
     */
    @Getter
    private final Method method;

    /**
     * The result of the method execution.
     */
    @Setter
    @Getter
    private Object result;

    /**
     * A flag indicating whether the method execution should be skipped.
     */
    @Setter
    private boolean skip;

    /**
     * Constructs a new MethodContext with specified details of the method execution.
     *
     * @param type        The class type where the method is declared.
     * @param target      The instance on which the method will be executed.
     * @param method      The method to be executed.
     * @param arguments   The arguments to be passed to the method.
     * @param description A description of the execution context.
     */
    public MethodContext(Class<?> type, Object target, Method method, Object[] arguments, String description) {
        super(type, arguments, description);
        super.setTarget(target);
        this.method = method;
    }

    /**
     * Checks if the method execution is set to be skipped.
     *
     * @see ExecutableContext#isSkip()
     */
    @Override
    public boolean isSkip() {
        return skip;
    }

    /**
     * Marks the execution as successful and sets the result.
     *
     * @param result The result of the successful execution.
     */
    public void success(Object result) {
        setResult(result);
        setThrowable(null);
    }

    @Override
    public String toString() {
        return "MethodContext{" +
                "type=" + type +
                ", method=" + method +
                ", arguments=" + Arrays.toString(arguments) +
                '}';
    }
}

