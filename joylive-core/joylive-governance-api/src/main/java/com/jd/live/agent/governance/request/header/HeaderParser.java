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

import com.jd.live.agent.core.util.tag.Label;
import com.jd.live.agent.governance.request.header.HeaderTraverse.MapHeaderTraverse;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A class that implements the {@link HeaderReader} and {@link HeaderWriter} interfaces.
 * It is designed to parse and manage headers using a generic type.
 *
 * @param <T> The type of the values used in the header parsing and management.
 */
public class HeaderParser<T> implements HeaderReader, HeaderWriter {

    protected final HeaderTraverse<T> traverse;

    protected final Function<T, List<String>> converter;

    protected final BiConsumer<String, String> consumer;

    public HeaderParser(HeaderTraverse<T> traverse, Function<T, List<String>> converter, BiConsumer<String, String> consumer) {
        this.traverse = traverse;
        this.converter = converter;
        this.consumer = consumer;
    }

    @Override
    public Iterator<String> getNames() {
        return traverse == null ? null : traverse.names();
    }

    @Override
    public List<String> getHeaders(String key) {
        T value = traverse == null ? null : traverse.get(key);
        return value == null || converter == null ? null : converter.apply(value);
    }

    @Override
    public String getHeader(String key) {
        return HeaderReader.super.getHeader(key);
    }

    @Override
    public void setHeader(String key, String value) {
        if (consumer != null) {
            consumer.accept(key, value);
        }
    }

    /**
     * A class that implements both {@link HeaderReader} and {@link HeaderWriter} interfaces.
     * This class is designed to handle the reading and writing of multi-map headers.
     */
    public static class MultiHeaderParser extends HeaderParser<List<String>> {

        private static final Function<List<String>, List<String>> LIST_LIST_FUNCTION = value -> value;

        public MultiHeaderParser(HeaderTraverse<List<String>> traverse) {
            super(traverse, LIST_LIST_FUNCTION, null);
        }

        public MultiHeaderParser(HeaderTraverse<List<String>> traverse, BiConsumer<String, String> consumer) {
            super(traverse, LIST_LIST_FUNCTION, consumer);
        }

        public static HeaderReader reader(HeaderTraverse<List<String>> traverse) {
            return new MultiHeaderParser(traverse);
        }

        public static HeaderReader reader(Map<String, List<String>> map) {
            return new MultiHeaderParser(new MapHeaderTraverse<>(map));
        }

        public static HeaderWriter writer(HeaderTraverse<List<String>> traverse, BiConsumer<String, String> consumer) {
            return new MultiHeaderParser(traverse, consumer);
        }

        public static HeaderWriter writer(Map<String, List<String>> map, BiConsumer<String, String> consumer) {
            return new MultiHeaderParser(new MapHeaderTraverse<>(map), consumer);
        }
    }

    /**
     * Represents a header that uses a map of objects to store and retrieve header values.
     * It implements both {@link HeaderReader} and {@link HeaderWriter} interfaces.
     */
    public static class ObjectHeaderParser extends HeaderParser<Object> {

        private static final Function<Object, List<String>> LIST_LIST_FUNCTION = value -> Label.parseValue(value.toString());

        public ObjectHeaderParser(HeaderTraverse<Object> traverse) {
            super(traverse, LIST_LIST_FUNCTION, null);
        }

        public ObjectHeaderParser(HeaderTraverse<Object> traverse, BiConsumer<String, String> consumer) {
            super(traverse, LIST_LIST_FUNCTION, consumer);
        }

        public static HeaderReader reader(HeaderTraverse<Object> traverse) {
            return new ObjectHeaderParser(traverse);
        }

        public static HeaderReader reader(Map<String, Object> map) {
            return new ObjectHeaderParser(new MapHeaderTraverse<>(map));
        }

        public static HeaderWriter writer(HeaderTraverse<Object> traverse, BiConsumer<String, String> consumer) {
            return new ObjectHeaderParser(traverse, consumer);
        }

        public static HeaderWriter writer(Map<String, Object> map, BiConsumer<String, String> consumer) {
            return new ObjectHeaderParser(new MapHeaderTraverse<>(map), consumer);
        }
    }

    /**
     * A class that implements both {@link HeaderReader} and {@link HeaderWriter} interfaces.
     * This class is designed to handle the reading and writing of headers using a map of strings.
     */
    public static class StringHeaderParser extends HeaderParser<String> {

        private static final Function<String, List<String>> STRING_LIST_FUNCTION = Label::parseValue;

        public StringHeaderParser(HeaderTraverse<String> traverse) {
            super(traverse, STRING_LIST_FUNCTION, null);
        }

        public StringHeaderParser(HeaderTraverse<String> traverse, BiConsumer<String, String> consumer) {
            super(traverse, STRING_LIST_FUNCTION, consumer);
        }

        public static HeaderReader reader(HeaderTraverse<String> traverse) {
            return new StringHeaderParser(traverse);
        }

        public static HeaderReader reader(Map<String, String> map) {
            return new StringHeaderParser(new MapHeaderTraverse<>(map));
        }

        public static HeaderWriter writer(HeaderTraverse<String> traverse, BiConsumer<String, String> consumer) {
            return new StringHeaderParser(traverse, consumer);
        }

        public static HeaderWriter writer(Map<String, String> map, BiConsumer<String, String> consumer) {
            return new StringHeaderParser(new MapHeaderTraverse<>(map), consumer);
        }
    }
}
