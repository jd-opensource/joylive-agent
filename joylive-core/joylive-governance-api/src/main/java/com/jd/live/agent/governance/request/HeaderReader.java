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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Interface for reading HTTP headers.
 * <p>
 * This interface defines methods to get header names, header values by key, and provides default methods
 * to get a single header value and to process header values using a function.
 */
public interface HeaderReader {
    /**
     * Returns an iterator over the names of all headers.
     *
     * @return An iterator over the header names.
     */
    Iterator<String> getNames();

    /**
     * Returns a list of all values for the specified header key.
     *
     * @param key The key of the header.
     * @return A list of values for the specified header key.
     */
    Iterable<String> getHeaders(String key);

    /**
     * Reads all the header names and values from this object and passes them to the given consumer.
     *
     * @param consumer a bi-consumer that accepts a header name and an iterable of header values
     */
    default void read(BiConsumer<String, Iterable<String>> consumer) {
        read(consumer, null);
    }

    /**
     * Reads all the header names and values from this object that match the given predicate and passes them to the given consumer.
     *
     * @param consumer  a bi-consumer that accepts a header name and an iterable of header values
     * @param predicate a predicate used to filter the header names; if null, all header names are included
     * @return the number of header names that were passed to the consumer
     */
    default int read(BiConsumer<String, Iterable<String>> consumer, Predicate<String> predicate) {
        int count = 0;
        if (consumer != null) {
            Iterator<String> names = getNames();
            String name;
            while (names.hasNext()) {
                name = names.next();
                if (predicate == null || predicate.test(name)) {
                    count++;
                    consumer.accept(name, getHeaders(name));
                }
            }
        }
        return count;
    }

    /**
     * Returns the first value for the specified header key.
     *
     * @param key The key of the header.
     * @return The first value for the specified header key, or null if the header is not present.
     */
    String getHeader(String key);

    /**
     * Returns the processed value for the specified header key using the provided function.
     *
     * @param key  The key of the header.
     * @param func The function to apply to the header value.
     * @return The processed value for the specified header key, or null if the header is not present.
     */
    default List<String> getHeader(String key, Function<String, List<String>> func) {
        String value = getHeader(key);
        return value == null ? null : func.apply(value);
    }

    /**
     * A class that implements the {@link HeaderReader} interface to read headers from a map of strings.
     */
    class SingleMapReader<T> implements HeaderReader {

        private final Map<String, T> map;

        public SingleMapReader(Map<String, T> map) {
            this.map = map;
        }

        @Override
        public Iterator<String> getNames() {
            return map == null ? null : map.keySet().iterator();
        }

        @Override
        public Iterable<String> getHeaders(String key) {
            T obj = map == null ? null : map.get(key);
            return obj == null ? null : Collections.singletonList(obj.toString());
        }

        @Override
        public String getHeader(String key) {
            T obj = map == null ? null : map.get(key);
            return obj == null ? null : obj.toString();
        }
    }

    /**
     * A class that implements the {@link HeaderReader} interface to read headers from a map of strings.
     */
    class StringMapReader extends SingleMapReader<String> {

        public StringMapReader(Map<String, String> map) {
            super(map);
        }
    }

    /**
     * A class that implements the {@link HeaderReader} interface to read headers from a map of objects.
     */
    class ObjectMapReader extends SingleMapReader<Object> {

        public ObjectMapReader(Map<String, Object> map) {
            super(map);
        }
    }

    /**
     * A class that implements the {@link HeaderReader} interface to read headers from a map of lists of strings.
     */
    class MultiValueMapReader implements HeaderReader {

        private final Map<String, List<String>> map;

        public MultiValueMapReader(Map<String, List<String>> map) {
            this.map = map;
        }

        @Override
        public Iterator<String> getNames() {
            return map.keySet().iterator();
        }

        @Override
        public Iterable<String> getHeaders(String key) {
            return map.get(key);
        }

        @Override
        public String getHeader(String key) {
            List<String> values = map.get(key);
            return values == null || values.isEmpty() ? null : values.get(0);
        }

    }

}
