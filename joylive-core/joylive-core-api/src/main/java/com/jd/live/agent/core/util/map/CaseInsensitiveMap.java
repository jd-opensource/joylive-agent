/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.core.util.map;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CaseInsensitiveMap<V> implements Map<String, V> {

    private final Map<String, V> delegate;

    public CaseInsensitiveMap() {
        this.delegate = new HashMap<>();
    }

    public CaseInsensitiveMap(int initialCapacity) {
        this.delegate = new HashMap<>(initialCapacity);
    }

    public CaseInsensitiveMap(Map<String, V> map) {
        this.delegate = new HashMap<>();
        putAll(map);
    }

    @Override
    public V get(Object key) {
        return key == null ? null : delegate.get(key.toString().toLowerCase());
    }

    @Override
    public V put(String key, V value) {
        return key == null ? null : delegate.put(key.toLowerCase(), value);
    }

    @Override
    public V remove(Object key) {
        return key == null ? null : delegate.remove(key.toString().toLowerCase());
    }

    @Override
    public boolean containsKey(Object key) {
        return key == null ? false : delegate.containsKey(key.toString().toLowerCase());
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> m) {
        if (m != null) {
            m.forEach(this::put);
        }
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public Set<String> keySet() {
        return delegate.keySet(); // 返回小写的key
    }

    @Override
    public Collection<V> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
        return delegate.entrySet(); // 返回小写key的entry
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Map && delegate.equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
