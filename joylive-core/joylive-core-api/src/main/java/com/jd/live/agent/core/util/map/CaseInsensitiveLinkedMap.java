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

import com.jd.live.agent.core.util.CollectionUtils;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.jd.live.agent.core.util.CollectionUtils.newHashMap;

/**
 * {@link LinkedHashMap} variant that stores String keys in a case-insensitive
 * manner, for example for key-based access in a results table.
 *
 * <p>Preserves the original order as well as the original casing of keys,
 * while allowing for contains, get and remove calls with any case of key.
 *
 * <p>Does <i>not</i> support {@code null} keys.
 *
 * @param <V> the value type
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.0
 */
public class CaseInsensitiveLinkedMap<V> implements Map<String, V>, Serializable, Cloneable {

    private final LinkedHashMap<String, V> delegate;

    private final HashMap<String, String> caseInsensitiveKeys;

    private final Locale locale;

    private transient volatile Set<String> keySet;

    private transient volatile Collection<V> values;

    private transient volatile Set<Entry<String, V>> entrySet;

    /**
     * Create a new CaseInsensitiveLinkedMap that stores case-insensitive keys
     * according to the default Locale (by default in lower case).
     *
     * @see #convertKey(String)
     */
    public CaseInsensitiveLinkedMap() {
        this((Locale) null);
    }

    /**
     * Create a new CaseInsensitiveLinkedMap that stores case-insensitive keys
     * according to the given Locale (in lower case).
     *
     * @param locale the Locale to use for case-insensitive key conversion
     * @see #convertKey(String)
     */
    public CaseInsensitiveLinkedMap(Locale locale) {
        this(12, locale);  // equivalent to LinkedHashMap's initial capacity of 16
    }

    /**
     * Create a new CaseInsensitiveLinkedMap that wraps a {@link LinkedHashMap}
     * with an initial capacity that can accommodate the specified number of
     * elements without any immediate resize/rehash operations to be expected,
     * storing case-insensitive keys according to the default Locale (in lower case).
     *
     * @param expectedSize the expected number of elements (with a corresponding
     *                     capacity to be derived so that no resize/rehash operations are needed)
     * @see #convertKey(String)
     */
    public CaseInsensitiveLinkedMap(int expectedSize) {
        this(expectedSize, null);
    }

    /**
     * Create a new CaseInsensitiveLinkedMap that wraps a {@link LinkedHashMap}
     * with an initial capacity that can accommodate the specified number of
     * elements without any immediate resize/rehash operations to be expected,
     * storing case-insensitive keys according to the given Locale (in lower case).
     *
     * @param expectedSize the expected number of elements (with a corresponding
     *                     capacity to be derived so that no resize/rehash operations are needed)
     * @param locale       the Locale to use for case-insensitive key conversion
     * @see #convertKey(String)
     */
    public CaseInsensitiveLinkedMap(int expectedSize, Locale locale) {
        this.delegate = new LinkedHashMap<String, V>(
                (int) (expectedSize / CollectionUtils.DEFAULT_LOAD_FACTOR), CollectionUtils.DEFAULT_LOAD_FACTOR) {
            @Override
            public boolean containsKey(Object key) {
                return CaseInsensitiveLinkedMap.this.containsKey(key);
            }

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
                boolean doRemove = CaseInsensitiveLinkedMap.this.removeEldestEntry(eldest);
                if (doRemove) {
                    removeCaseInsensitiveKey(eldest.getKey());
                }
                return doRemove;
            }
        };
        this.caseInsensitiveKeys = newHashMap(expectedSize);
        this.locale = (locale != null ? locale : Locale.getDefault());
    }

    /**
     * Copy constructor.
     */
    @SuppressWarnings("unchecked")
    private CaseInsensitiveLinkedMap(CaseInsensitiveLinkedMap<V> other) {
        this.delegate = (LinkedHashMap<String, V>) other.delegate.clone();
        this.caseInsensitiveKeys = (HashMap<String, String>) other.caseInsensitiveKeys.clone();
        this.locale = other.locale;
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
        return (key instanceof String && caseInsensitiveKeys.containsKey(convertKey((String) key)));
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public V get(Object key) {
        if (key instanceof String) {
            String caseInsensitiveKey = caseInsensitiveKeys.get(convertKey((String) key));
            if (caseInsensitiveKey != null) {
                return delegate.get(caseInsensitiveKey);
            }
        }
        return null;
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        if (key instanceof String) {
            String caseInsensitiveKey = caseInsensitiveKeys.get(convertKey((String) key));
            if (caseInsensitiveKey != null) {
                return delegate.get(caseInsensitiveKey);
            }
        }
        return defaultValue;
    }

    @Override
    public V put(String key, V value) {
        String oldKey = caseInsensitiveKeys.put(convertKey(key), key);
        V oldKeyValue = null;
        if (oldKey != null && !oldKey.equals(key)) {
            oldKeyValue = delegate.remove(oldKey);
        }
        V oldValue = delegate.put(key, value);
        return (oldKeyValue != null ? oldKeyValue : oldValue);
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> map) {
        if (map.isEmpty()) {
            return;
        }
        map.forEach(this::put);
    }

    @Override
    public V putIfAbsent(String key, V value) {
        String oldKey = caseInsensitiveKeys.putIfAbsent(convertKey(key), key);
        if (oldKey != null) {
            V oldKeyValue = delegate.get(oldKey);
            if (oldKeyValue != null) {
                return oldKeyValue;
            } else {
                key = oldKey;
            }
        }
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public V computeIfAbsent(String key, Function<? super String, ? extends V> mappingFunction) {
        String oldKey = caseInsensitiveKeys.putIfAbsent(convertKey(key), key);
        if (oldKey != null) {
            V oldKeyValue = delegate.get(oldKey);
            if (oldKeyValue != null) {
                return oldKeyValue;
            } else {
                key = oldKey;
            }
        }
        return delegate.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V remove(Object key) {
        if (key instanceof String) {
            String caseInsensitiveKey = removeCaseInsensitiveKey((String) key);
            if (caseInsensitiveKey != null) {
                return delegate.remove(caseInsensitiveKey);
            }
        }
        return null;
    }

    @Override
    public void clear() {
        caseInsensitiveKeys.clear();
        delegate.clear();
    }

    @Override
    public Set<String> keySet() {
        Set<String> result = keySet;
        if (result == null) {
            result = new KeySet(delegate.keySet());
            keySet = result;
        }
        return result;
    }

    @Override
    public Collection<V> values() {
        Collection<V> result = values;
        if (result == null) {
            result = new Values(delegate.values());
            values = result;
        }
        return result;
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
        Set<Entry<String, V>> result = entrySet;
        if (result == null) {
            result = new EntrySet(delegate.entrySet());
            entrySet = result;
        }
        return result;
    }

    @Override
    public CaseInsensitiveLinkedMap<V> clone() {
        return new CaseInsensitiveLinkedMap<>(this);
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

    /**
     * Return the locale used by this {@code CaseInsensitiveLinkedMap}.
     * Used for case-insensitive key conversion.
     *
     * @see #CaseInsensitiveLinkedMap(Locale)
     * @see #convertKey(String)
     * @since 4.3.10
     */
    public Locale getLocale() {
        return this.locale;
    }

    /**
     * Convert the given key to a case-insensitive key.
     * <p>The default implementation converts the key
     * to lower-case according to this Map's Locale.
     *
     * @param key the user-specified key
     * @return the key to use for storing
     * @see String#toLowerCase(Locale)
     */
    protected String convertKey(String key) {
        return key.toLowerCase(getLocale());
    }

    /**
     * Determine whether this map should remove the given eldest entry.
     *
     * @param eldest the candidate entry
     * @return {@code true} for removing it, {@code false} for keeping it
     */
    protected boolean removeEldestEntry(Entry<String, V> eldest) {
        return false;
    }

    private String removeCaseInsensitiveKey(String key) {
        return caseInsensitiveKeys.remove(convertKey(key));
    }

    private class KeySet extends AbstractSet<String> {

        private final Set<String> delegate;

        KeySet(Set<String> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            return this.delegate.contains(o);
        }

        @Override
        public Iterator<String> iterator() {
            return new KeySetIterator();
        }

        @Override
        public boolean remove(Object o) {
            return CaseInsensitiveLinkedMap.this.remove(o) != null;
        }

        @Override
        public void clear() {
            CaseInsensitiveLinkedMap.this.clear();
        }

        @Override
        public Spliterator<String> spliterator() {
            return this.delegate.spliterator();
        }

        @Override
        public void forEach(Consumer<? super String> action) {
            this.delegate.forEach(action);
        }
    }

    private class Values extends AbstractCollection<V> {

        private final Collection<V> delegate;

        Values(Collection<V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            return this.delegate.contains(o);
        }

        @Override
        public Iterator<V> iterator() {
            return new ValuesIterator();
        }

        @Override
        public void clear() {
            CaseInsensitiveLinkedMap.this.clear();
        }

        @Override
        public Spliterator<V> spliterator() {
            return this.delegate.spliterator();
        }

        @Override
        public void forEach(Consumer<? super V> action) {
            this.delegate.forEach(action);
        }
    }

    private class EntrySet extends AbstractSet<Entry<String, V>> {

        private final Set<Entry<String, V>> delegate;

        EntrySet(Set<Entry<String, V>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            return this.delegate.contains(o);
        }

        @Override
        public Iterator<Entry<String, V>> iterator() {
            return new EntrySetIterator();
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean remove(Object o) {
            if (this.delegate.remove(o)) {
                removeCaseInsensitiveKey(((Entry<String, V>) o).getKey());
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            this.delegate.clear();
            caseInsensitiveKeys.clear();
        }

        @Override
        public Spliterator<Entry<String, V>> spliterator() {
            return this.delegate.spliterator();
        }

        @Override
        public void forEach(Consumer<? super Entry<String, V>> action) {
            this.delegate.forEach(action);
        }
    }

    private abstract class EntryIterator<T> implements Iterator<T> {

        private final Iterator<Entry<String, V>> delegate;

        private Entry<String, V> last;

        EntryIterator() {
            this.delegate = CaseInsensitiveLinkedMap.this.delegate.entrySet().iterator();
        }

        protected Entry<String, V> nextEntry() {
            Entry<String, V> entry = this.delegate.next();
            this.last = entry;
            return entry;
        }

        @Override
        public boolean hasNext() {
            return this.delegate.hasNext();
        }

        @Override
        public void remove() {
            this.delegate.remove();
            if (this.last != null) {
                removeCaseInsensitiveKey(this.last.getKey());
                this.last = null;
            }
        }
    }

    private class KeySetIterator extends EntryIterator<String> {

        @Override
        public String next() {
            return nextEntry().getKey();
        }
    }

    private class ValuesIterator extends EntryIterator<V> {

        @Override
        public V next() {
            return nextEntry().getValue();
        }
    }

    private class EntrySetIterator extends EntryIterator<Entry<String, V>> {

        @Override
        public Entry<String, V> next() {
            return nextEntry();
        }
    }

}
