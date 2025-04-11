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

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CaseInsensitiveConcurrentMap<V> extends ConcurrentHashMap<String, V> implements Serializable {

    public CaseInsensitiveConcurrentMap() {
        super();
    }

    public CaseInsensitiveConcurrentMap(int initialCapacity) {
        super(initialCapacity);
    }

    public CaseInsensitiveConcurrentMap(Map<? extends String, ? extends V> m) {
        super(m);
    }

    public CaseInsensitiveConcurrentMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public CaseInsensitiveConcurrentMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        super(initialCapacity, loadFactor, concurrencyLevel);
    }

    @Override
    public V get(Object key) {
        return super.get(key.toString().toLowerCase());
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(key.toString().toLowerCase());
    }

    @Override
    public V put(String key, V value) {
        return super.put(key.toLowerCase(), value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> m) {
        for (Map.Entry<? extends String, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        return super.remove(key.toString().toLowerCase());
    }

    @Override
    public V putIfAbsent(String key, V value) {
        return super.putIfAbsent(key.toLowerCase(), value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return super.remove(key.toString().toLowerCase(), value);
    }

    @Override
    public boolean replace(String key, V oldValue, V newValue) {
        return super.replace(key.toLowerCase(), oldValue, newValue);
    }

    @Override
    public V replace(String key, V value) {
        return super.replace(key.toLowerCase(), value);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return super.getOrDefault(key.toString().toLowerCase(), defaultValue);
    }

    @Override
    public V computeIfAbsent(String key, Function<? super String, ? extends V> mappingFunction) {
        return super.computeIfAbsent(key.toLowerCase(), k -> mappingFunction.apply(key));
    }

    @Override
    public V computeIfPresent(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        return super.computeIfPresent(key.toLowerCase(), (k, v) -> remappingFunction.apply(key, v));
    }

    @Override
    public V compute(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        return super.compute(key.toLowerCase(), (k, v) -> remappingFunction.apply(key, v));
    }

    @Override
    public V merge(String key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return super.merge(key.toLowerCase(), value, remappingFunction);
    }

}
