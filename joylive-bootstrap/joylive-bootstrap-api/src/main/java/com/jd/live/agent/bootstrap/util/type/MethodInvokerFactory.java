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
                return MethodHandleFactory.getHandle(method);
            } catch (IllegalAccessException ignored) {
            }
        }
        return method::invoke;
    }

}
