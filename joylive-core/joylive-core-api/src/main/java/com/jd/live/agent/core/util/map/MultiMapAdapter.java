/*
 * Copyright 2002-2020 the original author or authors.
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


import java.io.Serializable;
import java.util.*;

import static com.jd.live.agent.core.util.CollectionUtils.newLinkedHashMap;

/**
 * Adapts a given {@link Map} to the {@link MultiMap} contract.
 *
 * @param <K> the key type
 * @param <V> the value element type
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see MultiLinkedMap
 * @since 5.3
 */
public class MultiMapAdapter<K, V> implements MultiMap<K, V>, Serializable {

    private final Map<K, List<V>> delegate;

    /**
     * Wrap the given target {@link Map} as a {@link MultiMap} adapter.
     *
     * @param targetMap the plain target {@code Map}
     */
    public MultiMapAdapter(Map<K, List<V>> targetMap) {
        this.delegate = targetMap;
    }

    @Override
    public V getFirst(K key) {
        List<V> values = delegate.get(key);
        return (values != null && !values.isEmpty() ? values.get(0) : null);
    }

    @Override
    public void add(K key, V value) {
        List<V> values = delegate.computeIfAbsent(key, k -> new ArrayList<>(1));
        values.add(value);
    }

    @Override
    public void addAll(K key, Collection<? extends V> values) {
        List<V> targets = delegate.computeIfAbsent(key, k -> new ArrayList<>(1));
        targets.addAll(values);
    }

    @Override
    public void addAll(MultiMap<K, V> values) {
        for (Entry<K, List<V>> entry : values.entrySet()) {
            addAll(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void set(K key, V value) {
        List<V> values = new ArrayList<>(1);
        values.add(value);
        delegate.put(key, values);
    }

    @Override
    public void setAll(K key, Collection<? extends V> values) {
        setAll(key, values, false);
    }

    @Override
    public void setAll(Map<K, V> values) {
        values.forEach(this::set);
    }

    @Override
    public Map<K, V> toSingleValueMap() {
        Map<K, V> singleValueMap = newLinkedHashMap(delegate.size());
        delegate.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                singleValueMap.put(key, values.get(0));
            }
        });
        return singleValueMap;
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
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public List<V> get(Object key) {
        return delegate.get(key);
    }

    @Override
    public List<V> put(K key, List<V> value) {
        return delegate.put(key, value);
    }

    @Override
    public List<V> remove(Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends List<V>> map) {
        delegate.putAll(map);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<List<V>> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<K, List<V>>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || delegate.equals(other));
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @SuppressWarnings("unchecked")
    protected void setAll(K key, Collection<? extends V> values, boolean zeroCopy) {
        List<V> targets;
        if (!zeroCopy || !(values instanceof ArrayList)) {
            targets = new ArrayList<>(values);
        } else {
            targets = (List<V>) values;
        }
        delegate.put(key, targets);
    }

}
