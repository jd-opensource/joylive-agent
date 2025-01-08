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
package com.jd.live.agent.governance.request.header;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents an interface for traversing headers with key-value pairs.
 *
 * @param <T> the type of the value associated with each header key
 */
public interface HeaderTraverse<T> {

    /**
     * Returns an iterator over the names of the headers.
     *
     * @return an iterator over the names
     */
    Iterator<String> names();

    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the key, or null if the key is not present
     */
    T get(String key);

    /**
     * Checks if the header is duplicable.
     * By default, this method returns {@code false}, indicating that the header is not duplicable.
     *
     * @return {@code true} if the header is duplicable, {@code false} otherwise
     */
    boolean isDuplicable();

    /**
     * A concrete implementation of HeaderTraverse that delegates the iteration of keys
     * to a provided Iterator and the retrieval of values to a provided Function.
     *
     * @param <T> the type of the value associated with each header key
     */
    class DelegateHeaderTraverse<T> implements HeaderTraverse<T> {

        private final Iterator<String> iterator;

        private final Function<String, T> function;

        private final boolean duplicable;

        public DelegateHeaderTraverse(Iterator<String> iterator, Function<String, T> function, boolean duplicable) {
            this.iterator = iterator;
            this.function = function;
            this.duplicable = duplicable;
        }

        @Override
        public Iterator<String> names() {
            return iterator;
        }

        @Override
        public T get(String key) {
            return function == null ? null : function.apply(key);
        }

        @Override
        public boolean isDuplicable() {
            return duplicable;
        }
    }

    /**
     * A concrete implementation of HeaderTraverse that uses a Map to store header key-value pairs.
     *
     * @param <T> the type of the value associated with each header key
     */
    class MapHeaderTraverse<T> extends DelegateHeaderTraverse<T> {

        public MapHeaderTraverse(Map<String, T> map, boolean duplicable) {
            super(map == null ? null : map.keySet().iterator(), map == null ? null : map::get, duplicable);
        }

    }
}
