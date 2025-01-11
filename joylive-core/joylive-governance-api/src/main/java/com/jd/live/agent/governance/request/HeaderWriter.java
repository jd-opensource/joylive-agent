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
package com.jd.live.agent.governance.request;

import com.jd.live.agent.core.util.tag.Label;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Interface for writing HTTP headers.
 * <p>
 * This interface defines a method to set a header with a specified key and value.
 */
public interface HeaderWriter {

    /**
     * Returns a list of all values for the specified header key.
     *
     * @param key The key of the header.
     * @return A list of values for the specified header key.
     */
    Iterable<String> getHeaders(String key);

    /**
     * Returns the first value for the specified header key.
     *
     * @param key The key of the header.
     * @return The first value for the specified header key, or null if the header is not present.
     */
    String getHeader(String key);

    /**
     * Checks if the header is duplicable.
     * By default, this method returns {@code false}, indicating that the header is not duplicable.
     *
     * @return {@code true} if the header is duplicable, {@code false} otherwise
     */
    boolean isDuplicable();

    /**
     * Add a header with the specified key and value.
     *
     * @param key   The key of the header.
     * @param value The value of the header.
     */
    void addHeader(String key, String value);

    /**
     * Sets a header with the specified key and value.
     *
     * @param key   The key of the header.
     * @param value The value of the header.
     */
    void setHeader(String key, String value);

    /**
     * Sets the headers with the specified key and list of values.
     * If the list of values is null or empty, the header is set to null.
     * If the list contains one value, that value is set as the header.
     * If the list contains multiple values, they are joined into a single string and set as the header.
     *
     * @param key    the header key
     * @param values the list of header values
     */
    default void setHeaders(String key, List<String> values) {
        int size = values == null ? 0 : values.size();
        switch (size) {
            case 0:
                setHeader(key, null);
                break;
            case 1:
                setHeader(key, values.get(0));
                break;
            default:
                setHeader(key, Label.join(values));
        }
    }

    /**
     * A class that implements the {@link HeaderWriter} interface to write headers to a map with single value.
     */
    abstract class SingleMapWriter<T> implements HeaderWriter {

        protected final Map<String, T> map;

        protected final BiConsumer<String, T> setter;

        public SingleMapWriter(Map<String, T> map) {
            this(map, null);
        }

        public SingleMapWriter(Map<String, T> map, BiConsumer<String, T> setter) {
            this.map = map;
            this.setter = setter;
        }

        @Override
        public Iterable<String> getHeaders(String key) {
            Object obj = map == null ? null : map.get(key);
            return obj == null ? null : Collections.singletonList(obj.toString());
        }

        @Override
        public String getHeader(String key) {
            T obj = map == null ? null : map.get(key);
            return obj == null ? null : obj.toString();
        }

        @Override
        public boolean isDuplicable() {
            return false;
        }

        @Override
        public void addHeader(String key, String value) {
            setHeader(key, value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setHeader(String key, String value) {
            if (setter != null) {
                // maybe the map is not initialized
                setter.accept(key, (T) value);
            } else if (map != null) {
                map.put(key, (T) value);
            }
        }
    }

    /**
     * A class that implements the {@link HeaderWriter} interface to write headers to a map of strings.
     */
    class StringMapWriter extends SingleMapWriter<String> {

        public StringMapWriter(Map<String, String> map) {
            super(map);
        }

        public StringMapWriter(Map<String, String> map, BiConsumer<String, String> setter) {
            super(map, setter);
        }
    }

    /**
     * A class that implements the {@link HeaderWriter} interface to write headers to a map of strings.
     */
    class ObjectMapWriter extends SingleMapWriter<Object> {

        public ObjectMapWriter(Map<String, Object> map) {
            super(map);
        }

        public ObjectMapWriter(Map<String, Object> map, BiConsumer<String, Object> setter) {
            super(map, setter);
        }
    }

    /**
     * A class that implements the {@link HeaderWriter} interface to write headers to a map of lists of strings.
     */
    class MultiValueMapWriter implements HeaderWriter {

        protected final Map<String, List<String>> map;

        public MultiValueMapWriter(Map<String, List<String>> map) {
            this.map = map;
        }

        @Override
        public Iterable<String> getHeaders(String key) {
            return map == null ? null : map.get(key);
        }

        @Override
        public String getHeader(String key) {
            List<String> values = map == null ? null : map.get(key);
            return values == null || values.isEmpty() ? null : values.get(0);
        }

        @Override
        public boolean isDuplicable() {
            return true;
        }

        @Override
        public void addHeader(String key, String value) {
            if (map != null) {
                map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            }
        }

        @Override
        public void setHeader(String key, String value) {
            if (map != null) {
                List<String> values = new ArrayList<>(1);
                values.add(value);
                map.put(key, values);
            }
        }
    }
}