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
package com.jd.live.agent.core.util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Utility class for performing atomic operations on {@link AtomicReference}.
 */
public class AtomicUtils {

    /**
     * Atomically updates the value of the given {@link AtomicReference} if the current value satisfies the specified predicate.
     * If the predicate is satisfied, the new value is set using {@link AtomicReference#compareAndSet(Object, Object)}.
     * If the update is successful and a success consumer is provided, the consumer is called with the old and new values.
     * If the update is successful, the new value is returned. Otherwise, the old value is returned.
     *
     * @param reference the {@link AtomicReference} to update
     * @param value     the new value to set
     * @param predicate a predicate that tests the current value; if null, the update is always attempted
     * @param success   a consumer that is called with the old and new values if the update is successful; if null, no consumer is called
     * @return the new value if the update was successful, otherwise the old value
     */
    public static <V> V update(AtomicReference<V> reference, V value, Predicate<V> predicate, BiConsumer<V, V> success) {
        V old;
        while (true) {
            old = reference.get();
            if (predicate == null || predicate.test(old)) {
                if (reference.compareAndSet(old, value)) {
                    if (success != null) {
                        success.accept(old, value);
                    }
                    return value;
                }
            } else {
                return old;
            }
        }
    }

    /**
     * Atomically gets or updates the value associated with the specified key in the given map.
     * If the key does not exist in the map, a new {@link AtomicReference} is created and associated with the key.
     * The method checks if the current value satisfies the specified predicate.
     * If the predicate is satisfied, the new value is set using {@link AtomicReference#compareAndSet(Object, Object)}.
     * If the update is successful and a success consumer is provided, the consumer is called with the old and new values.
     * If the update is successful, the new value is returned. Otherwise, the old value is returned.
     *
     * @param map       the map containing {@link AtomicReference} values
     * @param key       the key whose value is to be retrieved or updated
     * @param supplier  a supplier that provides the new value if the predicate is satisfied or the value is null
     * @param predicate a predicate that tests the current value; if null, the update is always attempted
     * @param success   a consumer that is called with the old and new values if the update is successful; if null, no consumer is called
     * @return the new value if the update was successful, otherwise the old value
     */
    public static <K, V> V getOrUpdate(Map<K, AtomicReference<V>> map, K key, Supplier<V> supplier, Predicate<V> predicate, BiConsumer<V, V> success) {
        AtomicReference<V> reference = map.computeIfAbsent(key, k -> new AtomicReference<>());
        V old = reference.get();
        if (predicate == null && old != null || predicate != null && predicate.test(old)) {
            return old;
        }
        return update(reference, supplier.get(), predicate == null ? null : predicate.negate(), success);
    }
}
