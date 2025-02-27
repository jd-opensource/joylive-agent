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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.StringUtils.CHAR_DOT;
import static com.jd.live.agent.core.util.StringUtils.splitList;

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
     * Looks up indices in the list of values where the predicate evaluates to true.
     * Iterates through the list with a specified step and adds the index to the result
     * if the predicate is satisfied. The iteration stops when the index reaches the specified length.
     *
     * @param <T>       the type of elements in the list
     * @param values    the list of values to search through
     * @param length    the number of elements to consider in the list
     * @param step      the step size for iterating through the list
     * @param predicate the predicate to test each element
     * @return a LookupIndex containing the indices where the predicate evaluates to true, or null if no indices are found
     */
    public static <T> LookupIndex lookup(List<T> values, int length, int step, Predicate<T> predicate) {
        LookupIndex result = null;
        for (int i = 0; i < length; i += step) {
            if (predicate.test(values.get(i))) {
                if (result == null) {
                    result = new LookupIndex();
                }
                result.add(i);
            }
        }
        return result;
    }

    /**
     * Looks up indices in the array of values where the predicate evaluates to true.
     * Iterates through the array with a specified step and adds the index to the result
     * if the predicate is satisfied. The iteration stops when the index reaches the specified length.
     *
     * @param <T>       the type of elements in the array
     * @param values    the list of values to search through
     * @param length    the number of elements to consider in the array
     * @param step      the step size for iterating through the array
     * @param predicate the predicate to test each element
     * @return a LookupIndex containing the indices where the predicate evaluates to true, or null if no indices are found
     */
    public static <T> LookupIndex lookup(T[] values, int length, int step, Predicate<T> predicate) {
        LookupIndex result = null;
        for (int i = 0; i < length; i += step) {
            if (predicate.test(values[i])) {
                if (result == null) {
                    result = new LookupIndex();
                }
                result.add(i);
            }
        }
        return result;
    }

    /**
     * Converts a value to a List.
     *
     * @param <T>   the type of elements in the iterator
     * @param value the value to convert
     * @return a List containing all the elements from the iterator, or null if the iterator is null
     */
    public static <T> List<T> toList(T value) {
        if (value == null) {
            return null;
        }
        List<T> result = new ArrayList<>(1);
        result.add(value);
        return result;
    }

    /**
     * Converts a value to a List.
     *
     * @param <T>   the type of elements in the iterator
     * @param value the value to convert
     * @return a List containing all the elements from the iterator, or null if the iterator is null
     */
    public static <T> List<T> singletonList(T value) {
        if (value == null) {
            return null;
        }
        return Collections.singletonList(value);
    }

    /**
     * Converts an Iterator to a List.
     *
     * @param <T>      the type of elements in the iterator
     * @param iterator the iterator to convert
     * @return a List containing all the elements from the iterator, or null if the iterator is null
     */
    public static <T> List<T> toList(Iterator<T> iterator) {
        List<T> result = null;
        if (iterator != null) {
            result = new ArrayList<>();
            while (iterator.hasNext()) {
                result.add(iterator.next());
            }
        }
        return result;
    }

    /**
     * Converts an Iterator to a List by applying a transformation function to each element.
     *
     * @param <T>      the type of elements in the iterator
     * @param <V>      the type of elements in the resulting list
     * @param iterator the iterator to convert
     * @param function the function to apply to each element of the iterator
     * @return a List containing the transformed elements from the iterator, or null if the iterator or function is null
     */
    public static <T, V> List<V> toList(Iterator<T> iterator, Function<T, V> function) {
        if (function == null) {
            throw new IllegalArgumentException("function is null");
        } else if (iterator == null) {
            return null;
        }
        List<V> result = new ArrayList<>();
        while (iterator.hasNext()) {
            result.add(function.apply(iterator.next()));
        }
        return result;
    }

    /**
     * Converts an iterable to a List.
     *
     * @param <T>      the type of elements in the iterable
     * @param iterable the iterable to convert
     * @return a List containing the elements from the iterable, or null if the iterable is null
     */
    public static <T> List<T> toList(Iterable<T> iterable) {
        if (iterable == null) {
            return null;
        }
        if (iterable instanceof List) {
            return (List<T>) iterable;
        }
        List<T> result = iterable instanceof Collection ? new ArrayList<>(((Collection<T>) iterable).size()) : new ArrayList<>();
        for (T t : iterable) {
            result.add(t);
        }
        return result;
    }

    /**
     * Converts an iterable to a List by applying a transformation function to each element.
     *
     * @param <T>      the type of elements in the iterable
     * @param <V>      the type of elements in the resulting list
     * @param iterable the iterable to convert
     * @param function the function to apply to each element of the iterable
     * @return a List containing the transformed elements from the iterable, or null if the iterable or function is null
     */
    public static <T, V> List<V> toList(Iterable<T> iterable, Function<T, V> function) {
        if (function == null) {
            throw new IllegalArgumentException("function is null");
        } else if (iterable == null) {
            return null;
        }
        List<V> result = iterable instanceof Collection ? new ArrayList<>(((Collection<T>) iterable).size()) : new ArrayList<>();
        for (T t : iterable) {
            V apply = function.apply(t);
            if (apply != null) {
                result.add(apply);
            }
        }
        return result;
    }

    /**
     * Converts an iterable to a List by applying a transformation function to each element.
     *
     * @param <T>      the type of elements in the iterable
     * @param <V>      the type of elements in the resulting list
     * @param arrays   the array to convert
     * @param function the function to apply to each element of the iterable
     * @return a List containing the transformed elements from the iterable, or null if the iterable or function is null
     */
    public static <T, V> List<V> toList(T[] arrays, Function<T, V> function) {
        if (function == null) {
            throw new IllegalArgumentException("function is null");
        } else if (arrays == null) {
            return null;
        }
        List<V> result = new ArrayList<>(arrays.length);
        for (T t : arrays) {
            result.add(function.apply(t));
        }
        return result;
    }

    /**
     * Converts an Enumeration to a List.
     *
     * @param <T>         the type of elements in the enumeration
     * @param enumeration the enumeration to convert
     * @return a List containing all the elements from the enumeration, or null if the enumeration is null
     */
    public static <T> List<T> toList(Enumeration<T> enumeration) {
        List<T> result = null;
        if (enumeration != null && enumeration.hasMoreElements()) {
            if (enumeration instanceof ListEnumeration) {
                result = ((ListEnumeration<T>) enumeration).getElements();
            } else {
                result = new ArrayList<>(2);
                while (enumeration.hasMoreElements()) {
                    result.add(enumeration.nextElement());
                }
            }
        }
        return result;
    }

    /**
     * Converts an Enumeration to an Iterator.
     *
     * @param <T>         the type of elements in the enumeration
     * @param enumeration the enumeration to convert
     * @return an Iterator containing all the elements from the enumeration, or null if the enumeration is null
     */
    public static <T> Iterator<T> toIterator(Enumeration<T> enumeration) {
        if (enumeration == null) {
            return null;
        }
        return new EnumerationIterator<>(enumeration);
    }

    /**
     * Converts an Iterator of type {@code V} to an Iterator of type {@code T} by applying a transformation function.
     *
     * @param <V>      the type of elements in the original iterator
     * @param <T>      the type of elements in the resulting iterator
     * @param iterator the original iterator
     * @param function the function to apply to each element of the original iterator
     * @return an Iterator of type {@code T} with elements transformed by the given function
     * @throws IllegalArgumentException if the function is null
     */
    public static <V, T> Iterator<T> toIterator(Iterator<V> iterator, Function<V, T> function) {
        if (function == null) {
            throw new IllegalArgumentException("function is null");
        } else if (iterator == null) {
            return null;
        }
        return new ConverterIterator<>(iterator, function);
    }

    /**
     * Converts an Iterator of type {@code V} to an Iterator of type {@code T} by applying a transformation function.
     *
     * @param <V>      the type of elements in the original iterator
     * @param <T>      the type of elements in the resulting iterator
     * @param arrays   the original array
     * @param function the function to apply to each element of the original iterator
     * @return an Iterator of type {@code T} with elements transformed by the given function
     * @throws IllegalArgumentException if the function is null
     */
    public static <V, T> Iterator<T> toIterator(V[] arrays, Function<V, T> function) {
        if (function == null) {
            throw new IllegalArgumentException("function is null");
        } else if (arrays == null) {
            return null;
        }
        return new ArrayIterator<>(arrays, function);
    }

    /**
     * Converts a List of values to an Enumeration.
     *
     * @param values the List of values to convert
     * @param <T>    the type of the values
     * @return an Enumeration containing the values from the List, or null if the List is null
     */
    public static <T> Enumeration<T> toEnumeration(List<T> values) {
        return values == null ? Collections.emptyEnumeration() : new ListEnumeration<>(values);
    }

    /**
     * Converts a Collection of values to an Enumeration.
     *
     * @param values the Collection of values to convert
     * @param <T>    the type of the values
     * @return an Enumeration containing the values from the Collection, or null if the Collection is null
     */
    public static <T> Enumeration<T> toEnumeration(Collection<T> values) {
        return values == null ? Collections.emptyEnumeration() : Collections.enumeration(values);
    }

    /**
     * Converts an iterable of elements into a map using specified key and value functions.
     * If the provided iterable is null, the method returns null.
     *
     * @param <T>           the type of elements in the iterable
     * @param <K>           the type of keys in the resulting map
     * @param <V>           the type of values in the resulting map
     * @param iterator      the iterable of elements to convert
     * @param keyFunction   the function to extract the key from each element
     * @param valueFunction the function to extract the value from each element
     * @return a map where each entry's key is the result of applying the keyFunction to an element,
     * and each entry's value is the result of applying the valueFunction to the same element
     */
    public static <T, K, V> Map<K, V> toMap(Iterable<T> iterator, Function<T, K> keyFunction, Function<T, V> valueFunction) {
        return toMap(iterator, null, keyFunction, valueFunction);
    }

    /**
     * Converts an iterable of elements into a map using specified key and value functions.
     * If the provided iterable is null, the method returns null.
     * The method applies a predicate to filter elements before converting them to map entries.
     * Entries with a null key are not added to the map.
     *
     * @param <T>           the type of elements in the iterable
     * @param <K>           the type of keys in the resulting map
     * @param <V>           the type of values in the resulting map
     * @param iterator      the iterable of elements to convert
     * @param predicate     the predicate to test each element; if null, all elements are included
     * @param keyFunction   the function to extract the key from each element
     * @param valueFunction the function to extract the value from each element
     * @return a map where each entry's key is the result of applying the keyFunction to an element,
     * and each entry's value is the result of applying the valueFunction to the same element
     */
    public static <T, K, V> Map<K, V> toMap(Iterable<T> iterator, Predicate<T> predicate, Function<T, K> keyFunction, Function<T, V> valueFunction) {
        if (iterator == null) {
            return null;
        }
        Map<K, V> result = new HashMap<>();
        for (T t : iterator) {
            if (predicate == null || predicate.test(t)) {
                result.put(keyFunction.apply(t), valueFunction.apply(t));
            }
        }
        return result;
    }

    /**
     * Converts an iterator of elements into a map using specified key and value functions.
     * If the provided iterator is null, the method returns null.
     * The method applies a predicate to filter elements before converting them to map entries.
     * Entries with a null key are not added to the map.
     *
     * @param <T>           the type of elements in the iterator
     * @param <K>           the type of keys in the resulting map
     * @param <V>           the type of values in the resulting map
     * @param iterator      the iterable of elements to convert
     * @param predicate     the predicate to test each element; if null, all elements are included
     * @param keyFunction   the function to extract the key from each element
     * @param valueFunction the function to extract the value from each element
     * @return a map where each entry's key is the result of applying the keyFunction to an element,
     * and each entry's value is the result of applying the valueFunction to the same element
     */
    public static <T, K, V> Map<K, V> toMap(Iterator<T> iterator, Predicate<T> predicate, Function<T, K> keyFunction, Function<T, V> valueFunction) {
        if (iterator == null) {
            return null;
        }
        Map<K, V> result = new HashMap<>();
        T t;
        while (iterator.hasNext()) {
            t = iterator.next();
            if (predicate == null || predicate.test(t)) {
                result.put(keyFunction.apply(t), valueFunction.apply(t));
            }
        }
        return result;
    }

    /**
     * Converts a flat map with dot-separated keys into a nested map structure.
     * Handles special cases such as keys representing lists (e.g., `a.b[0]`) .
     *
     * @param flatMap A flat map where keys are dot-separated strings representing nested paths.
     *                For example, `{"a.b.c": "value", "a.b[0]": "list_value"}`.
     * @return A nested map structure based on the keys in the input flat map.
     * For example, `{"a": {"b": {"c": "value", "0": "list_value"}}}`.
     * Returns `null` if the input `flatMap` is `null`.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> cascade(Map<String, Object> flatMap) {
        if (flatMap == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>(flatMap.size());
        Map<String, Object> parent;
        for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
            parent = result;
            String key = entry.getKey();
            Object value = entry.getValue();
            List<String> parts = splitList(key, c -> c == CHAR_DOT);
            int size = parts.size();
            int i = 0;
            Object v;
            List<Object> list;
            Integer index;
            int max = 0;
            int pos;
            for (String part : parts) {
                index = null;
                max = part.length() - 1;
                if (part.charAt(max) == ']') {
                    pos = part.lastIndexOf('[');
                    if (pos > 0 && pos < max) {
                        try {
                            index = Integer.parseInt(part.substring(pos + 1, max));
                            if (index >= 0) {
                                part = part.substring(0, pos);
                            } else {
                                index = null;
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                if (index != null) {
                    v = parent.get(part);
                    if (!(v instanceof List)) {
                        v = new ArrayList<>();
                        parent.put(part, v);
                    }
                    list = (List<Object>) v;
                    while (list.size() <= index) {
                        list.add(null);
                    }
                    list.set(index, i == size - 1 ? new HashMap<>() : value);
                } else if (i == size - 1) {
                    parent.put(part, value);
                } else {
                    v = parent.get(part);
                    if (!(v instanceof Map)) {
                        v = new HashMap<>();
                        parent.put(part, v);
                    }
                    parent = (Map<String, Object>) v;
                }
                i++;
            }
        }
        return result;
    }

    /**
     * Iterates over an iterable of elements, applying a predicate and a consumer to each element.
     * If the iterable or consumer is null, the method does nothing.
     * If the predicate is null, all elements are consumed.
     *
     * @param <T>       the type of elements in the iterable
     * @param iterable  the iterable of elements to iterate over
     * @param predicate the predicate to test each element; if null, all elements are consumed
     * @param consumer  the consumer to apply to each element that satisfies the predicate
     * @return the number of elements that satisfied the predicate and were consumed
     */
    public static <T> int iterate(Iterable<T> iterable, Predicate<T> predicate, Consumer<T> consumer) {
        if (iterable == null || consumer == null) {
            return 0;
        }
        int count = 0;
        for (T t : iterable) {
            if (predicate == null || predicate.test(t)) {
                count++;
                consumer.accept(t);
            }
        }
        return count;
    }

    /**
     * Iterates over an iterator of elements, applying a predicate and a consumer to each element.
     * If the iterator or consumer is null, the method does nothing and returns 0.
     * If the predicate is null, all elements are consumed.
     *
     * @param <T>       the type of elements in the iterator
     * @param iterable  the iterator of elements to iterate over
     * @param predicate the predicate to test each element; if null, all elements are consumed
     * @param consumer  the consumer to apply to each element that satisfies the predicate
     * @return the number of elements that satisfied the predicate and were consumed
     */
    public static <T> int iterate(Iterator<T> iterable, Predicate<T> predicate, Consumer<T> consumer) {
        if (iterable == null || consumer == null) {
            return 0;
        }
        int count = 0;
        T t;
        while (iterable.hasNext()) {
            t = iterable.next();
            if (predicate == null || predicate.test(t)) {
                count++;
                consumer.accept(t);
            }
        }
        return count;
    }

    /**
     * Iterates over the entries of the given map, applying the given predicate to each key and passing the matching key-value pairs to the given consumer.
     *
     * @param map       the map to iterate over
     * @param predicate a predicate used to filter the keys; if null, all keys are included
     * @param consumer  a bi-consumer that accepts a key and a value
     * @param <K>       the type of the keys in the map
     * @param <V>       the type of the values in the map
     * @return the number of key-value pairs that were passed to the consumer
     */
    public static <K, V> int iterate(Map<K, V> map, Predicate<K> predicate, BiConsumer<K, V> consumer) {
        int count = 0;
        K key;
        if (map != null && consumer != null) {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                key = entry.getKey();
                if (predicate == null || predicate.test(key)) {
                    count++;
                    consumer.accept(key, entry.getValue());
                }
            }
        }
        return count;
    }

    /**
     * Adds all elements from the source collection to the target collection that satisfy the given predicate.
     * If the predicate is null, all elements from the source collection are added to the target collection.
     * If either the source or target collection is null, this method does nothing.
     *
     * @param <T>       the type of elements in the collections
     * @param source    the source collection from which elements are to be added
     * @param target    the target collection to which elements are to be added
     * @param predicate the predicate to test elements before adding them to the target collection (can be null)
     */
    public static <T> void add(Collection<T> source, Collection<T> target, Predicate<T> predicate) {
        if (source == null || target == null) {
            return;
        }
        for (T t : source) {
            if (predicate == null || predicate.test(t)) {
                target.add(t);
            }
        }
    }

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

    /**
     * An Iterator that applies a transformation function to each element of another Iterator.
     *
     * @param <T> the type of elements returned by this iterator
     * @param <V> the type of elements in the original iterator
     */
    private static class ConverterIterator<T, V> implements Iterator<T> {

        private final Iterator<V> iterator;

        private final Function<V, T> function;

        ConverterIterator(Iterator<V> iterator, Function<V, T> function) {
            this.iterator = iterator;
            this.function = function;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public T next() {
            return function.apply(iterator.next());
        }
    }

    /**
     * An Iterator that wraps an Enumeration to provide Iterator functionality.
     *
     * @param <T> the type of elements in the enumeration
     */
    private static class EnumerationIterator<T> implements Iterator<T> {

        private final Enumeration<T> enumeration;

        EnumerationIterator(Enumeration<T> enumeration) {
            this.enumeration = enumeration;
        }

        @Override
        public boolean hasNext() {
            return enumeration.hasMoreElements();
        }

        @Override
        public T next() {
            return enumeration.nextElement();
        }
    }

    /**
     * An iterator that converts elements of an array from type {@code V} to type {@code T} using a provided function.
     *
     * @param <V> the type of elements in the input array
     * @param <T> the type of elements to be returned by the iterator
     */
    private static class ArrayIterator<V, T> implements Iterator<T> {

        /**
         * The array of elements to be iterated over.
         */
        private final V[] arrays;

        /**
         * The function used to convert elements from type {@code V} to type {@code T}.
         */
        private final Function<V, T> function;

        /**
         * The current index of the iterator.
         */
        private int index = 0;

        /**
         * Constructs a new ArrayConvertIterator with the specified array and conversion function.
         *
         * @param arrays   the array of elements to be iterated over
         * @param function the function to convert elements from type {@code V} to type {@code T}
         */
        ArrayIterator(V[] arrays, Function<V, T> function) {
            this.arrays = arrays;
            this.function = function;
        }

        @Override
        public boolean hasNext() {
            int length = arrays == null ? 0 : arrays.length;
            return index < length;
        }

        @Override
        public T next() {
            return function.apply(arrays[index++]);
        }
    }

    /**
     * A private static class that implements the Enumeration interface for a List of values.
     *
     * @param <T> the type of the values in the List
     */
    private static class ListEnumeration<T> implements Enumeration<T> {

        private final List<T> list;

        private int index = 0;

        ListEnumeration(List<T> list) {
            this.list = list;
        }

        @Override
        public boolean hasMoreElements() {
            return list != null && index < list.size();
        }

        @Override
        public T nextElement() {
            return list == null ? null : list.get(index++);
        }

        public List<T> getElements() {
            return index == 0 || list == null ? list : list.subList(index, list.size());
        }
    }

}
