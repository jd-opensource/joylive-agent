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

import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * CollectionUtils
 */
public class CollectionUtils {

    /**
     * Default load factor for {@link HashMap}/{@link LinkedHashMap} variants.
     */
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private static final Class<?> UNMODIFIED_MAP_CLASS = Collections.unmodifiableMap(new HashMap<>()).getClass();

    private static final FieldDesc MAP_FIELD = ClassUtils.describe(UNMODIFIED_MAP_CLASS).getFieldList().getField("m");

    /**
     * Filters the provided list of objects based on the given predicate.
     *
     * @param <T>       the type of objects in the list.
     * @param objects   the list of objects to be filtered.
     * @param predicate a predicate to test if a message passes the check.
     */
    public static <T> void filter(List<T> objects, Predicate<T> predicate) {
        int size = objects == null ? 0 : objects.size();
        if (size == 0) {
            return;
        }

        int writeIndex = 0;
        for (int readIndex = 0; readIndex < size; readIndex++) {
            T message = objects.get(readIndex);
            if (predicate == null || predicate.test(message)) {
                if (writeIndex != readIndex) {
                    objects.set(writeIndex, message);
                }
                writeIndex++;
            }
        }

        // Remove the remaining elements if any
        if (writeIndex < size) {
            objects.subList(writeIndex, size).clear();
        }
    }

    /**
     * Converts a list of source objects into a list of target objects using the provided converter function.
     *
     * @param <S>       The type of source objects.
     * @param <T>       The type of target objects.
     * @param sources   The list of source objects to convert.
     * @param converter The function to convert each source object into a target object.
     * @return A list of target objects.
     */
    public static <S, T> List<T> convert(List<S> sources, Function<S, T> converter) {
        if (sources == null || converter == null) {
            return new ArrayList<>();
        }
        List<T> result = new ArrayList<>(sources.size());
        for (S instance : sources) {
            result.add(converter.apply(instance));
        }
        return result;
    }

    /**
     * Returns a modified version of the given map, if necessary.
     *
     * @param sources the original map
     * @return the modified map, or the original map if no modification is needed
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> modifiedMap(Map<K, V> sources) {
        if (sources == null) {
            return null;
        }
        if (sources.getClass() == UNMODIFIED_MAP_CLASS) {
            sources = (Map<K, V>) MAP_FIELD.get(sources);
        }
        return sources;
    }

    /**
     * Instantiate a new {@link LinkedHashMap} with an initial capacity
     * that can accommodate the specified number of elements without
     * any immediate resize/rehash operations to be expected.
     */
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int expectedSize) {
        return new LinkedHashMap<>(computeMapInitialCapacity(expectedSize), DEFAULT_LOAD_FACTOR);
    }

    /**
     * Instantiate a new {@link HashMap} with an initial capacity
     * that can accommodate the specified number of elements without
     * any immediate resize/rehash operations to be expected.
     * <p>This differs from the regular {@link HashMap} constructor
     * which takes an initial capacity relative to a load factor
     * but is effectively aligned with the JDK's
     * {@link java.util.concurrent.ConcurrentHashMap#ConcurrentHashMap(int)}.
     *
     * @param expectedSize the expected number of elements (with a corresponding
     *                     capacity to be derived so that no resize/rehash operations are needed)
     * @see #newLinkedHashMap(int)
     * @since 5.3
     */
    public static <K, V> HashMap<K, V> newHashMap(int expectedSize) {
        return new HashMap<>(computeMapInitialCapacity(expectedSize), DEFAULT_LOAD_FACTOR);
    }

    private static int computeMapInitialCapacity(int expectedSize) {
        return (int) Math.ceil(expectedSize / (double) DEFAULT_LOAD_FACTOR);
    }
}
