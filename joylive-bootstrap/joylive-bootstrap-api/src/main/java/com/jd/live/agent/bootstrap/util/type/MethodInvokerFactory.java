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

import java.lang.reflect.Method;

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
        if (!method.isAccessible()) {
            Accessible.setAccessible(method, true);
        }
        if (Version.isJava7OrHigher()) {
            try {
                return MethodHandleFactory.getHandle(method);
            } catch (IllegalAccessException ignored) {
            }
        }
        return method::invoke;
    }

    /**
     * Creates a method invoker using Java reflection.
     *
     * @param method the method to create invoker for
     * @return a functional invoker that calls {@link Method#invoke}
     * @throws SecurityException if access cannot be granted
     */
    public static MethodInvoker getReflectInvoker(Method method) {
        if (!method.isAccessible()) {
            Accessible.setAccessible(method, true);
        }
        return method::invoke;
    }

    /**
     * Version detection utility for Java 7+ MethodHandle availability.
     */
    protected static class Version {

        // for test
        protected static boolean JAVA7_OR_HIGHER = detectJava7();

        private static boolean detectJava7() {
            try {
                Class.forName("java.lang.invoke.MethodHandle", true, ClassLoader.getSystemClassLoader());
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

        static boolean isJava7OrHigher() {
            return JAVA7_OR_HIGHER;
        }

    }

}
