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
import com.jd.live.agent.bootstrap.plugin.PluginEvent;
import com.jd.live.agent.bootstrap.plugin.PluginListener;
import com.jd.live.agent.bootstrap.plugin.PluginPublisher;
import com.jd.live.agent.bootstrap.plugin.definition.Interceptor;
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Manages advice interceptors with thread-safe operations.
 * Optimized for single interceptor scenarios with copy-on-write list.
 */
public class AdviceInterceptor implements PluginListener {
    /**
     * Unique advice identifier.
     */
    @Getter
    private final Object key;

    private final AtomicReference<MethodInterceptor> interceptors = new AtomicReference<>();

    /**
     * Tracks unique interceptor names to prevent duplicates.
     */
    private final Map<String, Boolean> names = new ConcurrentHashMap<>();

    /**
     * Thread-safe reference for locking operations.
     */
    private final AtomicReference<Object> reference = new AtomicReference<>();

    /**
     * Creates advice descriptor with specified key.
     *
     * @param key unique advice identifier
     */
    public AdviceInterceptor(Object key) {
        this.key = key;
    }

    /**
     * Adds interceptor with duplicate prevention.
     * Supports single interceptor optimization.
     *
     * @param interceptor interceptor to add
     * @return true if added successfully
     */
    public boolean add(Interceptor interceptor) {
        if (interceptor == null || names.putIfAbsent(interceptor.getClass().getCanonicalName(), Boolean.TRUE) != null) {
            return false;
        }
        while (true) {
            MethodInterceptor older = interceptors.get();
            MethodInterceptor newer;
            if (older == null) {
                newer = new MethodInterceptor(interceptor);
            } else {
                newer = older.add(interceptor);
            }
            if (interceptors.compareAndSet(older, newer)) {
                return true;
            }
        }
    }

    /**
     * Executes onEnter interceptors.
     *
     * @param context execution context
     * @param caller  operation with skip support
     * @throws Throwable if execution fails
     */
    public void onEnter(final ExecutableContext context, final SkippableCaller caller) throws Throwable {
        MethodInterceptor interceptor = interceptors.get();
        if (interceptor == null) {
            return;
        }
        InterceptorList enter = interceptor.enter;
        if (enter != null) {
            if (enter.interceptor != null) {
                caller.call(context, enter.interceptor);
            } else if (enter.interceptors != null) {
                for (int i = 0; i < enter.size; i++) {
                    if (caller.call(context, enter.interceptors[i])) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * Executes onExit interceptors with success/error routing.
     *
     * @param context   execution context
     * @param onSuccess success callback
     * @param onError   error callback
     * @param onExit    exit callback
     * @throws Throwable if execution fails
     */
    public void onExit(final ExecutableContext context, final Caller onSuccess, final Caller onError, final Caller onExit) throws Throwable {
        MethodInterceptor interceptor = interceptors.get();
        if (interceptor == null) {
            return;
        }
        if (context.isSuccess()) {
            if (interceptor.success != null) {
                interceptor.success.reverse(context, onSuccess);
            }
        } else {
            if (interceptor.error != null) {
                interceptor.error.reverse(context, onError);
            }
        }
        if (interceptor.exit != null) {
            interceptor.exit.reverse(context, onExit);
        }
    }

    /**
     * Locks advice to specific owner.
     * Registers as listener if owner is PluginPublisher.
     *
     * @param owner object attempting to lock
     * @return true if locked successfully
     */
    public boolean lock(Object owner) {
        if (owner == null) {
            return false;
        } else if (reference.compareAndSet(null, owner)) {
            if (owner instanceof PluginPublisher) {
                ((PluginPublisher) owner).addListener(this);
            }
            return true;
        } else {
            return reference.get() == owner;
        }
    }

    /**
     * Unlocks advice from current owner.
     *
     * @param owner current owner
     * @return true if unlocked successfully
     */
    protected boolean unlock(Object owner) {
        return owner != null && reference.compareAndSet(owner, null);
    }

    /**
     * Handles plugin uninstall events to remove advice.
     *
     * @param event plugin event
     */
    @Override
    public void onEvent(PluginEvent event) {
        if (event.getType() == PluginEvent.EventType.UNINSTALL && unlock(event.getOwner())) {
            AdviceHandler.remove(getKey());
        }
    }

    /**
     * Executes interceptor with context.
     */
    public interface Caller {

        /**
         * Executes interceptor operation.
         *
         * @param context     execution context
         * @param interceptor interceptor to execute
         * @throws Throwable if execution fails
         */
        void call(ExecutableContext context, Interceptor interceptor) throws Throwable;
    }

    /**
     * Executes interceptor with optional early termination.
     */
    public interface SkippableCaller {

        /**
         * Executes interceptor with skip support.
         *
         * @param context     execution context
         * @param interceptor interceptor to execute
         * @return true to stop iteration, false to continue
         * @throws Throwable if execution fails
         */
        boolean call(ExecutableContext context, Interceptor interceptor) throws Throwable;

    }

    /**
     * Manages interceptor lifecycle with enter/success/error/exit phases.
     */
    private static class MethodInterceptor {

        private InterceptorList enter;

        private InterceptorList success;

        private InterceptorList error;

        private InterceptorList exit;

        MethodInterceptor(InterceptorList enter, InterceptorList success, InterceptorList error, InterceptorList exit) {
            this.enter = enter;
            this.success = success;
            this.error = error;
            this.exit = exit;
        }

        MethodInterceptor(Interceptor interceptor) {
            add(interceptor, i -> enter = i, i -> success = i, i -> error = i, i -> exit = i);
        }

        /**
         * Adds interceptor to appropriate lifecycle phase.
         *
         * @param interceptor interceptor to add
         */
        public MethodInterceptor add(Interceptor interceptor) {
            AtomicReference<InterceptorList> enterRef = new AtomicReference<>(enter);
            AtomicReference<InterceptorList> successRef = new AtomicReference<>(success);
            AtomicReference<InterceptorList> errorRef = new AtomicReference<>(error);
            AtomicReference<InterceptorList> exitRef = new AtomicReference<>(exit);
            add(interceptor, enterRef::set, successRef::set, errorRef::set, exitRef::set);
            return new MethodInterceptor(enterRef.get(), successRef.get(), errorRef.get(), exitRef.get());
        }

        /**
         * Checks if interceptor has specific method.
         *
         * @param interceptor interceptor to check
         * @param methodName  method name to find
         * @param runnable    action to run if method exists
         */
        private void exist(Interceptor interceptor, String methodName, Runnable runnable) {
            try {
                Method method = interceptor.getClass().getMethod(methodName, ExecutableContext.class);
                if (method.getDeclaringClass() != Interceptor.class) {
                    runnable.run();
                }
            } catch (NoSuchMethodException e) {
                // ignore
            }
        }

        /**
         * Adds interceptor to list.
         *
         * @param target      existing list or null
         * @param interceptor interceptor to add
         * @return updated interceptor list
         */
        private InterceptorList add(InterceptorList target, Interceptor interceptor) {
            if (target == null) {
                return new InterceptorList(interceptor, null, 1);
            }
            return target.add(interceptor);
        }

        /**
         * Adds interceptor to lifecycle event lists based on implemented methods.
         * Checks if interceptor implements specific lifecycle methods and adds to corresponding lists.
         *
         * @param interceptor the interceptor to add
         * @param onEnter     consumer for enter event list
         * @param onSuccess   consumer for success event list
         * @param onError     consumer for error event list
         * @param onExit      consumer for exit event list
         */
        private void add(Interceptor interceptor,
                         Consumer<InterceptorList> onEnter,
                         Consumer<InterceptorList> onSuccess,
                         Consumer<InterceptorList> onError,
                         Consumer<InterceptorList> onExit) {
            exist(interceptor, "onEnter", () -> onEnter.accept(add(enter, interceptor)));
            exist(interceptor, "onSuccess", () -> onSuccess.accept(add(success, interceptor)));
            exist(interceptor, "onError", () -> onError.accept(add(error, interceptor)));
            exist(interceptor, "onExit", () -> onExit.accept(add(exit, interceptor)));
        }

    }

    /**
     * Thread-safe interceptor list implementing copy-on-write pattern for efficient concurrent access.
     * Supports both single interceptor optimization and dynamic array expansion.
     */
    private static class InterceptorList {

        private final Interceptor interceptor;

        /**
         * Thread-safe interceptor list using copy-on-write pattern.
         */
        private final Interceptor[] interceptors;

        private final int size;

        InterceptorList(Interceptor interceptor, Interceptor[] interceptors, int size) {
            this.interceptor = interceptor;
            this.interceptors = interceptors;
            this.size = size;
        }

        /**
         * Adds interceptor using copy-on-write pattern.
         * Handles single interceptor optimization and array expansion.
         *
         * @param interceptor interceptor to add
         * @return new interceptor list with added interceptor
         */
        public InterceptorList add(Interceptor interceptor) {
            if (this.interceptor != null) {
                Interceptor[] newArray = new Interceptor[4];
                newArray[0] = this.interceptor;
                newArray[1] = interceptor;
                return new InterceptorList(null, newArray, 2);
            } else if (interceptors == null) {
                return new InterceptorList(interceptor, null, 1);
            } else if (size >= interceptors.length) {
                Interceptor[] newArray = new Interceptor[interceptors.length * 2];
                System.arraycopy(interceptors, 0, newArray, 0, size);
                newArray[size] = interceptor;
                return new InterceptorList(null, newArray, size + 1);
            } else {
                Interceptor[] newArray = new Interceptor[interceptors.length];
                System.arraycopy(interceptors, 0, newArray, 0, size);
                newArray[size] = interceptor;
                return new InterceptorList(null, newArray, size + 1);
            }
        }

        /**
         * Executes operation on all interceptors in reverse order.
         *
         * @param context execution context
         * @param caller  operation to execute on each interceptor
         * @throws Throwable if any interceptor execution fails
         */
        public void reverse(final ExecutableContext context, final Caller caller) throws Throwable {
            if (interceptor != null) {
                caller.call(context, interceptor);
            } else if (interceptors != null) {
                for (int i = size - 1; i >= 0; i--) {
                    caller.call(context, interceptors[i]);
                }
            }
        }

    }
}
