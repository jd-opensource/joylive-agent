/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.implement.bytekit.bytebuddy.type;

import com.jd.live.agent.core.bytekit.type.TypePool;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A factory for creating and caching {@link TypePool} instances.
 * <p>
 * Since creating a TypePool can be somewhat expensive, this factory caches one instance
 * per ClassLoader to improve performance. This class is thread-safe.
 *
 * @since 1.9.0
 */
public class TypePoolFactory {

    // A cache mapping a ClassLoader to its dedicated TypePool.
    private static final ConcurrentMap<ClassLoader, TypePool> POOLS = new ConcurrentHashMap<>();

    // A placeholder for the bootstrap class loader, as its key in the map cannot be null.
    private static final ClassLoader BOOTSTRAP_CLASS_LOADER = new ClassLoader() {
    };

    /**
     * Gets a {@link TypePool} for the specified {@link ClassLoader}.
     * <p>
     * The returned instance is cached, so subsequent calls for the same class loader
     * will return the same instance.
     *
     * @param classLoader The class loader for which to get the type pool. Can be {@code null}
     *                    to represent the bootstrap class loader.
     * @return A non-null {@link TypePool} instance.
     */
    public static TypePool get(ClassLoader classLoader) {
        ClassLoader key = classLoader == null ? BOOTSTRAP_CLASS_LOADER : classLoader;
        // Atomically get or create the TypePool for the given class loader.
        return POOLS.computeIfAbsent(key, BuddyTypePool::new);
    }
}