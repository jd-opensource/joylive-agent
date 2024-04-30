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
package com.jd.live.agent.core.util.cache;

/**
 * A generic class that serves as a container for caching a single object of type T. This class provides a simple
 * way to store and retrieve the cached object, and it can be used in scenarios where a single value needs to be
 * shared or accessed across different parts of an application.
 *
 * <p>Note that the stored object is declared as volatile, which ensures that changes to the target object are
 * visible to all threads. However, this class does not provide any thread-safe operations for modifying the
 * target object. If multiple threads need to modify the cached object, external synchronization is required.</p>
 *
 * @param <T> The type of the object to be cached.
 */
public class CacheObject<T> {

    /**
     * The cached object of type T
     */
    protected volatile T target;

    /**
     * Default constructor. Initializes an empty CacheObject.
     */
    public CacheObject() {
    }

    /**
     * Constructor that initializes the CacheObject with a specific target object.
     *
     * @param target The object to be cached.
     */
    public CacheObject(T target) {
        this.target = target;
    }

    /**
     * Retrieves the cached object.
     *
     * @return The cached object of type T.
     */
    public T get() {
        return target;
    }

    /**
     * Static factory method that creates a new CacheObject with the specified target object.
     *
     * @param <T>    The type of the object to be cached.
     * @param target The object to be cached.
     * @return A new CacheObject containing the specified target object.
     */
    public static <T> CacheObject<T> of(T target) {
        return new CacheObject<>(target);
    }
}

