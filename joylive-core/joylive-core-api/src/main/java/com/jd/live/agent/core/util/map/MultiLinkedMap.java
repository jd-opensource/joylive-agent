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
import java.util.function.Supplier;

import static com.jd.live.agent.core.util.CollectionUtils.newLinkedHashMap;

/**
 * Simple implementation of {@link MultiMap} that wraps a {@link LinkedHashMap},
 * storing multiple values in an {@link ArrayList}.
 *
 * <p>This Map implementation is generally not thread-safe. It is primarily designed
 * for data structures exposed from request objects, for use in a single thread only.
 *
 * @param <K> the key type
 * @param <V> the value element type
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public class MultiLinkedMap<K, V> extends MultiMapAdapter<K, V>  // new public base class in 5.3
        implements Serializable, Cloneable {

    private static final long serialVersionUID = 3801124242820219131L;

    /**
     * Create a new MultiLinkedMap that wraps a {@link LinkedHashMap}.
     */
    public MultiLinkedMap() {
        super(new LinkedHashMap<>());
    }

    /**
     * Create a new MultiLinkedMap that wraps a {@link LinkedHashMap}
     * with an initial capacity that can accommodate the specified number of
     * elements without any immediate resize/rehash operations to be expected.
     *
     * @param expectedSize the expected number of elements (with a corresponding
     *                     capacity to be derived so that no resize/rehash operations are needed)
     */
    public MultiLinkedMap(int expectedSize) {
        super(newLinkedHashMap(expectedSize));
    }

    /**
     * Copy constructor: Create a new MultiLinkedMap with the same mappings as
     * the specified Map. Note that this will be a shallow copy; its value-holding
     * List entries will get reused and therefore cannot get modified independently.
     *
     * @param other the Map whose mappings are to be placed in this Map
     * @see #clone()
     * @see #deepCopy()
     */
    public MultiLinkedMap(Map<K, List<V>> other) {
        super(new LinkedHashMap<>(other));
    }

    /**
     * Constructs a new MultiLinkedMap with the specified initial capacity and load factor.
     *
     * @param creator a supplier that provides the initial map implementation
     */
    public MultiLinkedMap(Supplier<Map<K, List<V>>> creator) {
        super(creator.get());
    }

    /**
     * Constructs a new MultiLinkedMap with the specified initial capacity and load factor.
     *
     * @param other   the Map whose mappings are to be placed in this Map
     * @param creator a supplier that provides the initial map implementation
     */
    public MultiLinkedMap(Map<K, ? extends Collection<V>> other, Supplier<Map<K, List<V>>> creator) {
        super(creator.get());
        other.forEach(this::setAll);
    }

    /**
     * Create a deep copy of this Map.
     *
     * @return a copy of this Map, including a copy of each value-holding List entry
     * (consistently using an independent modifiable {@link ArrayList} for each entry)
     * along the lines of {@code MultiMap.addAll} semantics
     * @see #addAll(MultiMap)
     * @see #clone()
     * @since 4.2
     */
    public MultiLinkedMap<K, V> deepCopy() {
        MultiLinkedMap<K, V> copy = new MultiLinkedMap<>(size());
        forEach((key, values) -> copy.put(key, new ArrayList<>(values)));
        return copy;
    }

    /**
     * Create a regular copy of this Map.
     *
     * @return a shallow copy of this Map, reusing this Map's value-holding List entries
     * (even if some entries are shared or unmodifiable) along the lines of standard
     * {@code Map.put} semantics
     * @see #put(Object, List)
     * @see #putAll(Map)
     * @see MultiLinkedMap#MultiLinkedMap(Map)
     * @see #deepCopy()
     * @since 4.2
     */
    @Override
    public MultiLinkedMap<K, V> clone() {
        return new MultiLinkedMap<>(this);
    }

    /**
     * Creates a new case-insensitive MultiMap from the given map.
     *
     * @param other the original map
     * @return a new MultiMap with case-insensitive keys
     */
    public static MultiMap<String, String> caseInsensitive(Map<String, String> other) {
        MultiMap<String, String> result = new MultiLinkedMap<>(
                () -> other == null
                        ? new CaseInsensitiveLinkedMap<>()
                        : new CaseInsensitiveLinkedMap<>(other.size()));
        if (other != null) {
            result.setAll(other);
        }
        return result;
    }

}
