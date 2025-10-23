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
import java.util.concurrent.Callable;
import java.util.function.Function;

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
        this.target = target;
        this.method = method;
        this.origin = origin;
    }

    @Override
    public MethodContext setArgument(final int index, final Object value) {
        return (MethodContext) super.setArgument(index, value);
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

    public void skipWith(final ResultProvider provider) {
        provider.handle((result, throwable) -> {
            if (throwable != null) {
                skipWithThrowable(throwable);
            } else {
                skipWithResult(result);
            }
        });
    }

    public void skipWith(final Callable<Object> callable) {
        try {
            skipWithResult(callable.call());
        } catch (Throwable e) {
            skipWithThrowable(e);
        }
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

    @Override
    public String toString() {
        return description;
    }

    /**
     * Invokes the target method with the specified arguments.
     *
     * @return the result of the method invocation.
     * @throws Exception if any exception occurs during the method invocation.
     */
    public Object invokeOrigin() throws Exception {
        return invokeOrigin(target, method, arguments, (Function<Throwable, Exception>) null);
    }

    /**
     * Invokes the target method with the specified arguments and exception transformation.
     *
     * @param <T>     the return type of the method invocation
     * @param <E>     the type of exception that may be thrown
     * @param thrower function to transform exceptions, or null to use original exceptions
     * @return the result of the method invocation cast to type T
     * @throws E transformed or original exception if thrown during method invocation
     */
    public <T, E extends Exception> T invokeOrigin(Function<Throwable, E> thrower) throws E {
        return (T) invokeOrigin(target, method, arguments, thrower);
    }

    /**
     * Invokes the target method with the specified arguments.
     *
     * @param target the target.
     * @return the result of the method invocation.
     * @throws Exception if any exception occurs during the method invocation.
     */
    public Object invokeOrigin(final Object target) throws Exception {
        return invokeOrigin(target, method, arguments, (Function<Throwable, Exception>) null);
    }

    /**
     * Invokes the original method on target object .
     *
     * @param target    Target object to invoke method on
     * @param method    Method to invoke (will be made accessible if needed)
     * @param arguments Method arguments
     * @return Method return value
     * @throws Exception Original exception if thrown by target method
     */
    public static Object invokeOrigin(final Object target, Method method, Object[] arguments) throws Exception {
        return invokeOrigin(target, method, arguments, (Function<Throwable, Exception>) null);
    }

    /**
     * Invokes the original method on target object with exception transformation support.
     *
     * @param <T>       the type of exception that may be thrown
     * @param target    the target object to invoke method on
     * @param method    the method to invoke (will be made accessible if needed)
     * @param arguments the method arguments
     * @param thrower   function to transform exceptions, or null to use original exceptions
     * @return the method return value
     * @throws T transformed or original exception if thrown by target method
     */
    public static <T extends Exception> Object invokeOrigin(final Object target, Method method, Object[] arguments, Function<Throwable, T> thrower) throws T {
        try {
            OriginStack.push(target, method);
            // method is always a copy object by java.lang.Class.getMethods
            // so we need to set accessible to true
            Accessible.setAccessible(method, true);
            return method.invoke(target, arguments);
        } catch (Throwable e) {
            Throwable cause = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;
            if (thrower != null) {
                throw thrower.apply(cause);
            } else if (cause instanceof Exception) {
                throw (T) cause;
            } else if (e != cause && e instanceof Exception) {
                throw (T) cause;
            } else {
                throw new RuntimeException(cause.getMessage(), cause);
            }
        } finally {
            OriginStack.tryPop(target, method);
        }
    }

}

