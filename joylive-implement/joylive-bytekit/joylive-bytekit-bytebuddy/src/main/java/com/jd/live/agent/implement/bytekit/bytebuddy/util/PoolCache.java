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
package com.jd.live.agent.implement.bytekit.bytebuddy.util;

import net.bytebuddy.pool.TypePool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A cache that extends ConcurrentHashMap to store mappings of ClassLoader to TypePool.CacheProvider.
 * It also maintains a map of the last access time for each ClassLoader to facilitate cleanup.
 */
public class PoolCache extends ConcurrentHashMap<ClassLoader, TypePool.CacheProvider> {

    private final ConcurrentMap<ClassLoader, Long> lastAccessMap = new ConcurrentHashMap<>(16);

    public PoolCache() {
    }

    public PoolCache(int initialCapacity) {
        super(initialCapacity);
    }

    public PoolCache(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public PoolCache(int initialCapacity, float loadFactor, int concurrencyLevel) {
        super(initialCapacity, loadFactor, concurrencyLevel);
    }

    @Override
    public TypePool.CacheProvider get(Object key) {
        if (key == null) {
            return null;
        }
        TypePool.CacheProvider result = super.get(key);
        if (result != null) {
            lastAccessMap.putIfAbsent((ClassLoader) key, System.currentTimeMillis());
            return result;
        }
        return null;
    }

    /**
     * Cleans the cache by removing entries that have not been accessed
     * within the specified expiration time.
     *
     * @param expireTime the time in milliseconds after which an entry is considered expired
     */
    public void recycle(long expireTime) {
        List<ClassLoader> expires = new ArrayList<>(16);
        long now = System.currentTimeMillis();
        lastAccessMap.forEach((key, value) -> {
            if (value + expireTime < now) {
                expires.add(key);
            }
        });
        for (ClassLoader key : expires) {
            lastAccessMap.remove(key);
            remove(key);
        }
    }
}
