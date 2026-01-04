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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages advice interceptors with thread-safe operations.
 * Optimized for single interceptor scenarios with copy-on-write list.
 */
public class AdviceDesc implements PluginListener {
    /**
     * Unique advice identifier.
     */
    @Getter
    private final Object key;

    private final AtomicReference<CopyOnWriteInterceptorList> interceptors = new AtomicReference<>();

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
    public AdviceDesc(Object key) {
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
            CopyOnWriteInterceptorList older = interceptors.get();
            CopyOnWriteInterceptorList newer;
            if (older == null) {
                newer = new CopyOnWriteInterceptorList(interceptor, null, 1);
            } else {
                newer = older.add(interceptor);
            }
            if (interceptors.compareAndSet(older, newer)) {
                return true;
            }
        }
    }

    /**
     * Executes operation on all interceptors.
     *
     * @param context execution context
     * @param caller  operation to execute
     * @throws Throwable if execution fails
     */
    public void iterate(ExecutableContext context, Caller caller) throws Throwable {
        CopyOnWriteInterceptorList ai = interceptors.get();
        if (ai == null) {
            return;
        }
        if (ai.interceptor != null) {
            caller.call(context, ai.interceptor);
        } else if (ai.interceptors != null) {
            for (int i = 0; i < ai.size; i++) {
                caller.call(context, ai.interceptors[i]);
            }
        }
    }

    /**
     * Executes operation with early termination support.
     *
     * @param context execution context
     * @param caller  operation with skip support
     * @throws Throwable if execution fails
     */
    public void iterate(ExecutableContext context, SkippableCaller caller) throws Throwable {
        CopyOnWriteInterceptorList ai = interceptors.get();
        if (ai == null) {
            return;
        }
        if (ai.interceptor != null) {
            caller.call(context, ai.interceptor);
        } else if (ai.interceptors != null) {
            for (int i = 0; i < ai.size; i++) {
                if (caller.call(context, ai.interceptors[i])) {
                    return;
                }
            }
        }
    }

    /**
     * Executes operation in reverse order.
     *
     * @param context execution context
     * @param caller  operation to execute
     * @throws Throwable if execution fails
     */
    public void reverse(ExecutableContext context, Caller caller) throws Throwable {
        CopyOnWriteInterceptorList ai = interceptors.get();
        if (ai == null) {
            return;
        }
        if (ai.interceptor != null) {
            caller.call(context, ai.interceptor);
        } else if (ai.interceptors != null) {
            for (int i = ai.size - 1; i >= 0; i--) {
                caller.call(context, ai.interceptors[i]);
            }
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
     * Functional interface for interceptor execution.
     */
    public interface Caller {
        void call(ExecutableContext context, Interceptor interceptor) throws Throwable;
    }

    /**
     * Functional interface for skippable interceptor execution.
     */
    public interface SkippableCaller {
        boolean call(ExecutableContext context, Interceptor interceptor) throws Throwable;
    }

    private static class CopyOnWriteInterceptorList {

        private final Interceptor interceptor;

        /**
         * Thread-safe interceptor list using copy-on-write pattern.
         */
        private final Interceptor[] interceptors;

        private final int size;

        CopyOnWriteInterceptorList(Interceptor interceptor, Interceptor[] interceptors, int size) {
            this.interceptor = interceptor;
            this.interceptors = interceptors;
            this.size = size;
        }

        /**
         * Adds interceptor with duplicate prevention.
         * Supports single interceptor optimization.
         *
         * @param interceptor interceptor to add
         * @return new advice interceptor
         */
        public CopyOnWriteInterceptorList add(Interceptor interceptor) {
            if (this.interceptor != null) {
                Interceptor[] newArray = new Interceptor[4];
                newArray[0] = this.interceptor;
                newArray[1] = interceptor;
                return new CopyOnWriteInterceptorList(null, newArray, 2);
            } else if (interceptors == null) {
                return new CopyOnWriteInterceptorList(interceptor, null, 1);
            } else if (size >= interceptors.length) {
                Interceptor[] newArray = new Interceptor[interceptors.length * 2];
                System.arraycopy(interceptors, 0, newArray, 0, size);
                newArray[size] = interceptor;
                return new CopyOnWriteInterceptorList(null, newArray, size + 1);
            } else {
                Interceptor[] newArray = new Interceptor[interceptors.length];
                System.arraycopy(interceptors, 0, newArray, 0, size);
                newArray[size] = interceptor;
                return new CopyOnWriteInterceptorList(null, newArray, size + 1);
            }
        }

    }
}
