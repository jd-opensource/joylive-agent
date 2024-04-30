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
package com.jd.live.agent.core.util.map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A builder class that implements the {@link MapBuilder} interface to construct a map from a list of items.
 * It uses a {@link Supplier} to provide the list of items, a {@link Consumer} to consume each item (optional),
 * a {@link Function} to extract a key from each item, and another {@link Function} to convert the key (optional).
 *
 * @param <K> The type of the keys in the map.
 * @param <T> The type of the items in the list and values in the map.
 */
public class ListBuilder<K, T> implements MapBuilder<K, T> {
    /**
     * The supplier used to provide the list of items.
     */
    protected final Supplier<List<T>> supplier;

    /**
     * The consumer used to perform an action on each item (optional).
     */
    protected final Consumer<T> consumer;

    /**
     * The function used to extract a key from each item.
     */
    protected final Function<T, K> keyFunc;

    /**
     * The function used to convert the extracted key (optional).
     */
    protected final Function<K, K> keyConverter;

    /**
     * Constructs a new ListBuilder with the specified supplier and key function.
     *
     * @param supplier The supplier to provide the list of items.
     * @param keyFunc  The function to extract a key from each item.
     */
    public ListBuilder(Supplier<List<T>> supplier, Function<T, K> keyFunc) {
        this(supplier, null, keyFunc, null);
    }

    /**
     * Constructs a new ListBuilder with the specified supplier, consumer, and key function.
     *
     * @param supplier The supplier to provide the list of items.
     * @param consumer The consumer to perform an action on each item.
     * @param keyFunc The function to extract a key from each item.
     */
    public ListBuilder(Supplier<List<T>> supplier, Consumer<T> consumer,
                       Function<T, K> keyFunc) {
        this(supplier, consumer, keyFunc, null);
    }

    /**
     * Constructs a new ListBuilder with the specified supplier, consumer, key function, and key converter.
     *
     * @param supplier The supplier to provide the list of items.
     * @param consumer The consumer to perform an action on each item.
     * @param keyFunc The function to extract a key from each item.
     * @param keyConverter The function to convert the extracted key.
     */
    public ListBuilder(Supplier<List<T>> supplier, Consumer<T> consumer,
                       Function<T, K> keyFunc, Function<K, K> keyConverter) {
        this.supplier = supplier;
        this.consumer = consumer;
        this.keyFunc = keyFunc;
        this.keyConverter = keyConverter;
    }

    @Override
    public Function<K, K> getKeyConverter() {
        return keyConverter;
    }

    @Override
    public Map<K, T> build() {
        List<T> items = supplier == null ? null : supplier.get();
        Map<K, T> result = items == null ? new HashMap<>() : new HashMap<>(items.size());
        if (items != null && keyFunc != null) {
            K key;
            for (T item : items) {
                if (consumer != null)
                    consumer.accept(item);
                key = keyFunc.apply(item);
                key = keyConverter == null ? key : keyConverter.apply(key);
                if (key != null) {
                    result.put(key, item);
                }
            }
        }
        return result;
    }
}

