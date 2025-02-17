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

import com.jd.live.agent.bootstrap.util.type.Accessible;
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

    @Getter
    private final boolean origin;

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
     * @param origin      A flag to indicate that it will invoke origin method.
     */
    public MethodContext(final Class<?> type, final Object target, final Method method,
                         final Object[] arguments, final String description, final boolean origin) {
        super(type, arguments, description);
        super.setTarget(target);
        this.method = method;
        this.origin = origin;
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

    public void skipWithResult(final Object result) {
        setResult(result);
        setSkip(true);
    }

    public void skipWithThrowable(final Throwable throwable) {
        setThrowable(throwable);
        setSkip(true);
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
    public void success(final Object result) {
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
    public Object invokeOrigin(final Object target) throws Exception {
        try {
            OriginStack.push(target, method);
            // method is always a copy object by java.lang.Class.getMethods
            // so we need to set accessible to true
            Accessible.setAccessible(method, true);
            return method.invoke(target, arguments);
        } catch (InvocationTargetException e) {
            if (e.getCause() != null && e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        } finally {
            OriginStack.tryPop(target, method);
        }
    }

    @Override
    public String toString() {
        return description;
    }

}

