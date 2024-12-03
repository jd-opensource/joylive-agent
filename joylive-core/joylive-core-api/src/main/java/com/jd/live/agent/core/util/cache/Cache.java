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

import java.util.function.Supplier;

/**
 * Represents a generic cache system interface, providing essential operations such as retrieving, checking size,
 * and clearing the cache. The cache is designed to store key-value pairs, where keys and values can be of any type
 * specified by the generic parameters {@code K} and {@code T} respectively.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <T> the type of mapped values
 */
public interface Cache<K, T> {

    /**
     * Retrieves the value associated with the specified key in this cache.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the specified key, or {@code null} if this cache contains no mapping for the key
     */
    T get(K key);

    /**
     * Retrieves the value associated with the specified key, or returns a default value if the key is not present in the cache.
     * This is a convenience method that provides a simple way to retrieve a value, supplying a default if the key is not found.
     *
     * @param key          the key whose associated value is to be returned
     * @param defaultValue the default value to return if the key is not found in the cache
     * @return the value associated with the specified key or {@code defaultValue} if the key is not found
     */
    default T get(K key, T defaultValue) {
        T result = get(key);
        return result == null ? defaultValue : result;
    }

    /**
     * Retrieves the value associated with the specified key, or returns a default value if the key is not present in the cache.
     * This is a convenience method that provides a simple way to retrieve a value, supplying a default if the key is not found.
     *
     * @param key             the key whose associated value is to be returned
     * @param defaultSupplier the default value supplier
     * @return the value associated with the specified key or {@code defaultValue} if the key is not found
     */
    default T get(K key, Supplier<T> defaultSupplier) {
        T result = get(key);
        if (result == null) {
            result = defaultSupplier == null ? null : defaultSupplier.get();
        }
        return result;
    }

    /**
     * Checks if the cache contains no elements.
     *
     * @return {@code true} if this cache contains no key-value mappings, {@code false} otherwise
     */
    boolean isEmpty();

    /**
     * Returns the number of key-value mappings in this cache.
     *
     * @return the number of key-value mappings in this cache
     */
    int size();

    /**
     * Removes all of the mappings from this cache. The cache will be empty after this call returns.
     */
    void clear();

}

