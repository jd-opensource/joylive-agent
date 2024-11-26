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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    public void skip() {
        setSkip(true);
    }

    public void skipWithResult(Object result) {
        setResult(result);
        skip();
    }

    public void skipWithThrowable(Throwable throwable) {
        setThrowable(throwable);
        skip();
    }

    @SuppressWarnings("unchecked")
    public <T> T getResult() {
        return (T) result;
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

    /**
     * Invokes the target method with the specified arguments.
     *
     * @return the result of the method invocation.
     * @throws Exception if any exception occurs during the method invocation.
     */
    public Object invokeOrigin() throws Exception {
        return invokeOrigin(target);
    }

    /**
     * Invokes the target method with the specified arguments.
     *
     * @param target the target.
     * @return the result of the method invocation.
     * @throws Exception if any exception occurs during the method invocation.
     */
    public Object invokeOrigin(Object target) throws Exception {
        try {
            markOrigin();
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            return method.invoke(target, arguments);
        } catch (InvocationTargetException e) {
            if (e.getCause() != null) {
                return e.getCause();
            }
            return e;
        } finally {
            getAndRemoveOrigin();
        }
    }

    @Override
    public String toString() {
        return description;
    }
}

