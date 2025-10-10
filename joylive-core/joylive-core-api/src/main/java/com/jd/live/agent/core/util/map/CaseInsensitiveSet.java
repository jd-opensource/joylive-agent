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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A case-insensitive Set implementation for String elements.
 * All string operations (add, contains, remove) are performed in a case-insensitive manner.
 * Internally stores strings in lowercase for consistent comparison.
 *
 */
public class CaseInsensitiveSet implements Set<String> {

    private final Set<String> delegate;

    public CaseInsensitiveSet() {
        this.delegate = new HashSet<>();
    }

    public CaseInsensitiveSet(int initialCapacity) {
        this.delegate = new HashSet<>(initialCapacity);
    }

    public CaseInsensitiveSet(Collection<? extends String> collection) {
        this.delegate = new HashSet<>();
        addAll(collection);
    }

    @Override
    public boolean add(String s) {
        return s == null ? false : delegate.add(s.toLowerCase());
    }

    @Override
    public boolean remove(Object o) {
        return o == null ? false : delegate.remove(o.toString().toLowerCase());
    }

    @Override
    public boolean contains(Object o) {
        return o == null ? false : delegate.contains(o.toString().toLowerCase());
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        if (c == null || c.isEmpty()) {
            return false;
        }
        boolean modified = false;
        for (String s : c) {
            if (add(s)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c == null || c.isEmpty()) {
            return false;
        }
        boolean modified = false;
        for (Object o : c) {
            if (remove(o)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c == null) {
            boolean modified = !isEmpty();
            clear();
            return modified;
        }

        Set<String> lowercases = new HashSet<>(c.size());
        for (Object o : c) {
            if (o instanceof String) {
                lowercases.add(((String) o).toLowerCase());
            }
        }
        return delegate.retainAll(lowercases);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c == null || c.isEmpty()) {
            return true;
        }
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
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
    public Iterator<String> iterator() {
        return delegate.iterator(); // Returns lowercase strings
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Set && delegate.equals(obj);
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
