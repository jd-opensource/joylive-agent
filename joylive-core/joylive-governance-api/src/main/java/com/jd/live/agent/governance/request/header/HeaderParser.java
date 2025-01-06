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
import com.jd.live.agent.governance.context.bag.Carrier;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A class that implements the {@link HeaderReader} and {@link HeaderWriter} interfaces.
 * It is designed to parse and manage headers using a generic type.
 *
 * @param <T> The type of the values used in the header parsing and management.
 */
public class HeaderParser<T> implements HeaderReader, HeaderWriter {

    protected final WrappedMap<T> map;

    protected final Function<T, List<String>> converter;

    protected final BiConsumer<String, String> consumer;

    protected final Supplier<String> idSupplier;

    public HeaderParser(WrappedMap<T> map, Function<T, List<String>> converter, BiConsumer<String, String> consumer, Supplier<String> idSupplier) {
        this.map = map;
        this.converter = converter;
        this.consumer = consumer;
        this.idSupplier = idSupplier;
    }

    @Override
    public Iterator<String> getHeaderNames() {
        return map == null ? null : map.keyIterator();
    }

    @Override
    public List<String> getHeaders(String key) {
        T value = map == null ? null : map.get(key);
        return value == null || converter == null ? null : converter.apply(value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        if (idSupplier == null || idSupplier.get() == null)
            return null;
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put(Carrier.ATTRIBUTE_RESTORE_BY, idSupplier.get());
        return attributes;
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

        public MultiHeaderParser(WrappedMap<List<String>> map) {
            super(map, LIST_LIST_FUNCTION, null, null);
        }

        public MultiHeaderParser(WrappedMap<List<String>> map, Supplier<String> idSupplier) {
            super(map, LIST_LIST_FUNCTION, null, idSupplier);
        }

        public MultiHeaderParser(WrappedMap<List<String>> map, BiConsumer<String, String> consumer) {
            super(map, LIST_LIST_FUNCTION, consumer, null);
        }

        public static HeaderReader reader(Map<String, List<String>> map) {
            return new MultiHeaderParser(WrappedMap.ofMap(map));
        }

        public static HeaderReader reader(WrappedMap<List<String>> wrappedMap) {
            return new MultiHeaderParser(wrappedMap);
        }

        public static HeaderReader reader(Map<String, List<String>> map, Supplier<String> idSupplier) {
            return new MultiHeaderParser(WrappedMap.ofMap(map), idSupplier);
        }

        public static HeaderReader reader(WrappedMap<List<String>> wrappedMap, Supplier<String> idSupplier) {
            return new MultiHeaderParser(wrappedMap, idSupplier);
        }

        public static HeaderWriter writer(BiConsumer<String, String> consumer) {
            return new MultiHeaderParser(WrappedMap.ofMap(new HashMap<>()), consumer);
        }

        public static HeaderWriter writer(Map<String, List<String>> map, BiConsumer<String, String> consumer) {
            return new MultiHeaderParser(WrappedMap.ofMap(map), consumer);
        }
    }

    /**
     * Represents a header that uses a map of objects to store and retrieve header values.
     * It implements both {@link HeaderReader} and {@link HeaderWriter} interfaces.
     */
    public static class ObjectHeaderParser extends HeaderParser<Object> {

        private static final Function<Object, List<String>> LIST_LIST_FUNCTION = value -> Label.parseValue(value.toString());

        public ObjectHeaderParser(WrappedMap<Object> map) {
            super(map, LIST_LIST_FUNCTION, null, null);
        }

        public ObjectHeaderParser(WrappedMap<Object> map, Supplier<String> idSupplier) {
            super(map, LIST_LIST_FUNCTION, null, idSupplier);
        }

        public ObjectHeaderParser(WrappedMap<Object> map, BiConsumer<String, String> consumer) {
            super(map, LIST_LIST_FUNCTION, consumer, null);
        }

        public static HeaderReader reader(Map<String, Object> map) {
            return new ObjectHeaderParser(WrappedMap.ofMap(map));
        }

        public static HeaderReader reader(WrappedMap<Object> wrappedMap) {
            return new ObjectHeaderParser(wrappedMap);
        }

        public static HeaderReader reader(Map<String, Object> map, Supplier<String> idSupplier) {
            return new ObjectHeaderParser(WrappedMap.ofMap(map), idSupplier);
        }

        public static HeaderReader reader(WrappedMap<Object> wrappedMap, Supplier<String> idSupplier) {
            return new ObjectHeaderParser(wrappedMap, idSupplier);
        }

        public static HeaderWriter writer(BiConsumer<String, String> consumer) {
            return new ObjectHeaderParser(WrappedMap.ofMap(new HashMap<>()), consumer);
        }

        public static HeaderWriter writer(Map<String, Object> map, BiConsumer<String, String> consumer) {
            return new ObjectHeaderParser(WrappedMap.ofMap(map), consumer);
        }
    }

    /**
     * A class that implements both {@link HeaderReader} and {@link HeaderWriter} interfaces.
     * This class is designed to handle the reading and writing of headers using a map of strings.
     */
    public static class StringHeaderParser extends HeaderParser<String> {

        private static final Function<String, List<String>> STRING_LIST_FUNCTION = Label::parseValue;


        public StringHeaderParser(WrappedMap<String> map) {
            super(map, STRING_LIST_FUNCTION, null, null);
        }

        public StringHeaderParser(WrappedMap<String> map, Supplier<String> idSupplier) {
            super(map, STRING_LIST_FUNCTION, null, idSupplier);
        }

        public StringHeaderParser(WrappedMap<String> map, BiConsumer<String, String> consumer) {
            super(map, STRING_LIST_FUNCTION, consumer, null);
        }

        public static HeaderReader reader(Map<String, String> map) {
            return new StringHeaderParser(WrappedMap.ofMap(map));
        }

        public static HeaderReader reader(WrappedMap<String> wrappedMap) {
            return new StringHeaderParser(wrappedMap);
        }

        public static HeaderReader reader(Map<String, String> map, Supplier<String> idSupplier) {
            return new StringHeaderParser(WrappedMap.ofMap(map), idSupplier);
        }

        public static HeaderReader reader(WrappedMap<String> wrappedMap, Supplier<String> idSupplier) {
            return new StringHeaderParser(wrappedMap, idSupplier);
        }

        public static HeaderWriter writer(BiConsumer<String, String> consumer) {
            return new StringHeaderParser(WrappedMap.ofMap(new HashMap<>()), consumer);
        }

        public static HeaderWriter writer(Map<String, String> map, BiConsumer<String, String> consumer) {
            return new StringHeaderParser(WrappedMap.ofMap(map), consumer);
        }
    }

    public abstract static class WrappedMap<T> {

        public abstract Iterator<String> keyIterator();

        public abstract T get(String key);

        // 将 ofMap 定义为静态方法
        public static <T> WrappedMap<T> ofMap(Map<String, T> map) {
            // 返回一个匿名子类实现
            return new WrappedMap<T>() {
                @Override
                public Iterator<String> keyIterator() {
                    return map.keySet().iterator();
                }

                @Override
                public T get(String key) {
                    return map.get(key);
                }
            };
        }
    }
}
