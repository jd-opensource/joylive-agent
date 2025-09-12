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
import com.jd.live.agent.core.bytekit.type.TypeDesc;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link TypePool} implementation that is backed by Byte Buddy's {@link net.bytebuddy.pool.TypePool}.
 * It uses a {@link ClassFileLocator} to find class files from a given {@link ClassLoader} and
 * parses them into {@link TypeDescription} objects, which are then wrapped as {@link BuddyTypeDesc}.
 * This implementation is thread-safe and caches the results.
 *
 * @since 1.9.0
 */
public class BuddyTypePool implements TypePool {

    private final ClassLoader classLoader;

    // The underlying, high-performance type pool from the Byte Buddy library.
    private final net.bytebuddy.pool.TypePool byteBuddyTypePool;

    // A cache for our wrapped BuddyTypeDesc objects to avoid repeated wrapping.
    private final ConcurrentMap<String, TypeDesc> cache = new ConcurrentHashMap<>(8192);

    /**
     * Constructs a new BuddyTypePool for a given ClassLoader.
     *
     * @param classLoader The ClassLoader to search for .class files.
     */
    public BuddyTypePool(ClassLoader classLoader) {
        this.classLoader = classLoader;
        // Create a ClassFileLocator that can find class files using the provided ClassLoader.
        ClassFileLocator locator = ClassFileLocator.ForClassLoader.of(classLoader);
        // Create a default, caching Byte Buddy TypePool.
        this.byteBuddyTypePool = net.bytebuddy.pool.TypePool.Default.of(locator);
    }

    @Override
    public TypeDesc describe(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        // Use computeIfAbsent for efficient, thread-safe, and atomic "check-then-act" caching.
        return cache.computeIfAbsent(name, this::findDescription);
    }

    /**
     * The actual lookup logic that is called when a type is not found in the cache.
     *
     * @param name The name of the type to find.
     * @return The described type or null if not found.
     */
    private TypeDesc findDescription(String name) {
        try {
            // Use the Byte Buddy pool to describe the type. This reads and parses the .class file.
            net.bytebuddy.pool.TypePool.Resolution resolution = byteBuddyTypePool.describe(name);
            if (resolution.isResolved()) {
                // If successful, get the TypeDescription.
                TypeDescription typeDescription = resolution.resolve();
                // Wrap it in our BuddyTypeDesc, passing 'this' as the pool for future lookups.
                return new BuddyTypeDesc(typeDescription, this);
            }
        } catch (Exception e) {
            // In case of any error during parsing, we treat it as "not found".
            // Log the exception if you need to debug class loading issues.
        }
        return null;
    }
}