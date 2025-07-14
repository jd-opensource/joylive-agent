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
 * Factory for creating and caching MethodHandle instances.
 * Thread-safe implementation using concurrent hash map.
 */
public class MethodHandleFactory {

    private static final Map<MethodSignature, MethodHandleCaller> CACHE = new ConcurrentHashMap<>();
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
    public static MethodHandleCaller getHandle(Method method) throws IllegalAccessException {
        MethodSignature key = new MethodSignature(method);
        MethodHandleCaller execution = CACHE.get(key);
        if (execution == null) {
            MethodHandle handle = Modifier.isPublic(method.getModifiers()) ? PUBLIC_LOOKUP.unreflect(method) : LOOKUP.unreflect(method);
            MethodHandleCaller caller = new MethodHandleCaller(handle, Modifier.isStatic(method.getModifiers()));
            execution = CACHE.computeIfAbsent(key, k -> caller);
        }
        return execution;
    }
}
