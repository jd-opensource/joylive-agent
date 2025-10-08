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

import com.jd.live.agent.core.util.map.MapBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A generic cache implementation that uses a {@link Map} to store key-value pairs. This class provides
 * methods for retrieving values based on keys, checking if the cache is empty, and getting the size of the
 * cache. It also supports a {@link MapBuilder} to customize the creation of the underlying map and to
 * apply a key conversion function if needed.
 *
 * <p>This class ensures that the underlying map is built lazily and that access to the map is thread-safe.
 * The map is only built once, and subsequent calls to the cache methods operate on the same map instance.</p>
 *
 * @param <K> The type of the keys in the cache.
 * @param <T> The type of the values in the cache.
 */
public class MapCache<K, T> implements Cache<K, T> {
    /**
     * The builder used to create and configure the underlying map.
     */
    protected MapBuilder<K, T> cacheBuilder;

    /**
     * The volatile map that holds the key-value pairs. It is lazily initialized and accessed in a thread-safe manner.
     */
    protected volatile Map<K, T> cache;

    /**
     * Constructs a new MapCache with the specified builder.
     *
     * @param cacheBuilder The builder to use for creating and configuring the underlying map.
     */
    public MapCache(MapBuilder<K, T> cacheBuilder) {
        this.cacheBuilder = cacheBuilder;
    }

    @Override
    public T get(K key) {
        if (key == null) {
            return null;
        }
        key = convert(key);
        return getCache().get(key);
    }

    @Override
    public boolean isEmpty() {
        return getCache().isEmpty();
    }

    @Override
    public int size() {
        return getCache().size();
    }

    @Override
    public void clear() {
        synchronized (this) {
            cache = null;
        }
    }

    /**
     * Returns the underlying cache map. If the map has not been initialized, it is built lazily and in a thread-safe manner.
     *
     * @return The underlying cache map.
     */
    protected Map<K, T> getCache() {
        if (cache == null) {
            synchronized (this) {
                if (cache == null) {
                    cache = build();
                }
            }
        }
        return cache;
    }

    /**
     * Converts the key using the key conversion function provided by the cache builder, if any.
     *
     * @param key The key to convert.
     * @return The converted key.
     */
    protected K convert(K key) {
        Function<K, K> keyConverter = cacheBuilder == null ? null : cacheBuilder.getKeyConverter();
        key = keyConverter == null || key == null ? key : keyConverter.apply(key);
        return key;
    }

    /**
     * Builds the underlying map using the cache builder. If no builder is provided, a default HashMap is created.
     *
     * @return The built map.
     */
    protected Map<K, T> build() {
        Map<K, T> result = cacheBuilder == null ? new HashMap<>() : cacheBuilder.build();
        return result == null ? new HashMap<>() : result;
    }

}

