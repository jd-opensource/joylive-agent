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
package com.jd.live.agent.bootstrap.util.type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating optimized method invokers using either MethodHandle (Java 7+) or reflection.
 */
public class MethodInvokerFactory {

    /**
     * Gets an optimized method invoker based on Java version.
     * Uses MethodHandle for Java 7+, falls back to reflection otherwise.
     *
     * @param method the method to invoke
     * @return appropriate invoker implementation
     */
    public static MethodInvoker getInvoker(Method method) {
        return getInvoker(method, MethodVersion.AUTO);
    }

    /**
     * Gets an optimized method invoker appropriate for the runtime environment.
     *
     * @param method the method to create an invoker for
     * @param version the method version support checker
     * @return optimized method invoker (MethodHandle-based if supported)
     */
    public static MethodInvoker getInvoker(Method method, MethodVersion version) {
        if (!method.isAccessible()) {
            Accessible.setAccessible(method, true);
        }
        if (version != null && version.supportMethodHandle()) {
            try {
                return MethodHandleCache.getHandle(method);
            } catch (IllegalAccessException ignored) {
            }
        }
        return method::invoke;
    }

    /**
     * Factory for creating and caching MethodHandle instances.
     * Thread-safe implementation using concurrent hash map.
     */
    private static class MethodHandleCache {

        private static final Map<Method, MethodInvoker> CACHE =
                new ConcurrentHashMap<>(256, 0.9f, 32);
        private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
        private static final MethodHandles.Lookup PUBLIC_LOOKUP = MethodHandles.publicLookup();

        /**
         * Gets or creates a MethodHandle for the specified method.
         * Caches handles to avoid repeated lookup operations.
         *
         * @param method the reflection method to convert
         * @return the corresponding MethodHandle
         * @throws IllegalAccessException if method access is restricted
         */
        public static MethodInvoker getHandle(Method method) throws IllegalAccessException {
            MethodInvoker result = CACHE.get(method);
            if (result == null) {
                MethodHandle handle = Modifier.isPublic(method.getModifiers()) ? PUBLIC_LOOKUP.unreflect(method) : LOOKUP.unreflect(method);
                MethodInvoker caller = Modifier.isStatic(method.getModifiers()) ? createStaticCaller(method, handle) : createInstanceCaller(method, handle);
                result = CACHE.computeIfAbsent(method, k -> caller);
            }
            return result;
        }

        /**
         * Creates a specialized static method caller based on parameter count.
         * Uses optimized caller implementations for methods with 0-10 parameters,
         * falls back to generic caller for methods with more parameters.
         *
         * @param method the target method to create caller for
         * @param handle the method handle for the target method
         * @return specialized static method caller instance
         */
        private static MethodInvoker createStaticCaller(Method method, MethodHandle handle) {
            switch (method.getParameterCount()) {
                case 0:
                    return (target, args) -> handle.invoke();
                case 1:
                    return (target, args) -> handle.invoke(args[0]);
                case 2:
                    return (target, args) -> handle.invoke(args[0], args[1]);
                case 3:
                    return (target, args) -> handle.invoke(args[0], args[1], args[2]);
                case 4:
                    return (target, args) -> handle.invoke(args[0], args[1], args[2], args[3]);
                case 5:
                    return (target, args) -> handle.invoke(args[0], args[1], args[2], args[3], args[4]);
                case 6:
                    return (target, args) -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5]);
                case 7:
                    return (target, args) -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
                case 8:
                    return (target, args) -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
                case 9:
                    return (target, args) -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
                case 10:
                    return (target, args) -> handle.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9]);
                default:
                    return (target, args) -> handle.asSpreader(Object[].class, args.length).invoke(args);
            }
        }

        /**
         * Creates a specialized instance method caller based on parameter count.
         * Uses optimized caller implementations for methods with 0-10 parameters,
         * falls back to generic caller for methods with more parameters.
         *
         * @param method the target method to create caller for
         * @param handle the method handle for the target method
         * @return specialized instance method caller instance
         */
        private static MethodInvoker createInstanceCaller(Method method, MethodHandle handle) {
            switch (method.getParameterCount()) {
                case 0:
                    return (target, args) -> handle.invoke(target);
                case 1:
                    return (target, args) -> handle.invoke(target, args[0]);
                case 2:
                    return (target, args) -> handle.invoke(target, args[0], args[1]);
                case 3:
                    return (target, args) -> handle.invoke(target, args[0], args[1], args[2]);
                case 4:
                    return (target, args) -> handle.invoke(target, args[0], args[1], args[2], args[3]);
                case 5:
                    return (target, args) -> handle.invoke(target, args[0], args[1], args[2], args[3], args[4]);
                case 6:
                    return (target, args) -> handle.invoke(target, args[0], args[1], args[2], args[3], args[4], args[5]);
                case 7:
                    return (target, args) -> handle.invoke(target, args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
                case 8:
                    return (target, args) -> handle.invoke(target, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
                case 9:
                    return (target, args) -> handle.invoke(target, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
                case 10:
                    return (target, args) -> handle.invoke(target, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9]);
                default:
                    return (target, args) -> {
                        Object[] newArgs = new Object[args.length + 1];
                        newArgs[0] = target;
                        System.arraycopy(args, 0, newArgs, 1, args.length);
                        return handle.invoke(newArgs);
                    };
            }
        }

    }
}
