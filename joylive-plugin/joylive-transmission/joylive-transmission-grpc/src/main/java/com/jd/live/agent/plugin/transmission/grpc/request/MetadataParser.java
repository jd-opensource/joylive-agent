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
package com.jd.live.agent.plugin.transmission.grpc.request;

import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory;
import com.jd.live.agent.core.util.KeyValue;
import com.jd.live.agent.core.util.LookupIndex;
import com.jd.live.agent.governance.request.HeaderFeature;
import com.jd.live.agent.governance.request.HeaderParser;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.CollectionUtils.lookup;
import static com.jd.live.agent.core.util.CollectionUtils.singletonList;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

public class MetadataParser implements HeaderParser {

    private static final Map<String, Key<String>> KEYS = new ConcurrentHashMap<>();

    private final Metadata metadata;

    public MetadataParser(Metadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public Iterator<String> getNames() {
        return metadata.keys().iterator();
    }

    @Override
    public Iterable<String> getHeaders(String key) {
        return metadata.getAll(getOrCreate(key));
    }

    @Override
    public String getHeader(String key) {
        return metadata.get(getOrCreate(key));
    }

    @Override
    public int read(BiConsumer<String, Iterable<String>> consumer, Predicate<String> predicate) {
        int length = 2 * FieldGetter.INSTANCE.getSize(metadata);
        Object[] namesAndValues = FieldGetter.INSTANCE.getNamesAndValues(metadata);
        int count = 0;
        String name;
        Object value;
        for (int i = 0; i < length; i++) {
            name = new String((byte[]) namesAndValues[i]);
            value = namesAndValues[++i];
            if (predicate == null || predicate.test(name)) {
                count++;
                consumer.accept(name, value == null ? null : singletonList(new String((byte[]) value, StandardCharsets.UTF_8)));
            }
        }
        return count;
    }

    @Override
    public HeaderFeature getFeature() {
        return HeaderFeature.DUPLICABLE_BATCHABLE;
    }

    @Override
    public void addHeader(String key, String value) {
        metadata.put(getOrCreate(key), value);
    }

    @Override
    public void setHeader(String key, String value) {
        if (value == null) {
            return;
        }

        int length = 2 * FieldGetter.INSTANCE.getSize(metadata);
        Object[] namesAndValues = FieldGetter.INSTANCE.getNamesAndValues(metadata);

        Key<String> metaKey = getOrCreate(key);
        // update single
        if (!updateSingle(metaKey, value, namesAndValues, length)) {
            // update multi
            updateMulti(metaKey, value);
        }
    }

    @Override
    public void setHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        int length = 2 * FieldGetter.INSTANCE.getSize(metadata);
        Object[] namesAndValues = FieldGetter.INSTANCE.getNamesAndValues(metadata);

        List<KeyValue<Key<String>, String>> multiKeys = null;
        Key<String> metaKey;
        String key;
        String value;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            metaKey = getOrCreate(key);
            if (!updateSingle(metaKey, value, namesAndValues, length)) {
                if (multiKeys == null) {
                    multiKeys = new ArrayList<>();
                }
                multiKeys.add(new KeyValue<>(metaKey, value));
            }
        }
        if (multiKeys != null) {
            multiKeys.forEach(multiKey -> updateMulti(multiKey.getKey(), multiKey.getValue()));
        }
    }

    /**
     * Updates the value associated with a given key in the array of names and values.
     * If the key is not found, it adds the key and value to the metadata map.
     * If the key is found once, it updates the value in the array.
     * If the key is found multiple times, it returns false.
     *
     * @param key       the key to update or add
     * @param value     the value to associate with the key
     * @param keyValues the array of names and values to search through
     * @param length    the number of elements to consider in the array
     * @return true if the update or addition was successful, false if the key was found multiple times
     */
    private boolean updateSingle(Key<String> key, String value, Object[] keyValues, int length) {

        byte[] bytes = FieldGetter.INSTANCE.getNameBytes(key);

        LookupIndex index = lookup(keyValues, length, 2, o -> Arrays.equals((byte[]) o, bytes));
        int size = index == null ? 0 : index.size();
        if (size == 0) {
            // add
            metadata.put(key, value);
            return true;
        } else if (size == 1) {
            // most is only one
            keyValues[index.getIndex() + 1] = ASCII_STRING_MARSHALLER.toAsciiString(value).getBytes(StandardCharsets.US_ASCII);
            return true;
        }
        return false;
    }

    /**
     * Updates the value associated with a given key in the metadata map.
     * First, it removes all existing entries for the key, then adds the new key-value pair.
     *
     * @param key   the key to update
     * @param value the new value to associate with the key
     */
    private void updateMulti(Key<String> key, String value) {
        metadata.removeAll(key);
        metadata.put(key, value);
    }

    /**
     * Retrieves the Metadata.Key for the given key, creating it if it does not already exist.
     *
     * @param key the key to retrieve or create
     * @return the Metadata.Key for the given key
     */
    private static Metadata.Key<String> getOrCreate(String key) {
        return KEYS.computeIfAbsent(key, k -> Metadata.Key.of(k, ASCII_STRING_MARSHALLER));
    }

    /**
     * A utility class to access private fields of the {@link Metadata} class.
     */
    private static class FieldGetter {

        /**
         * The singleton instance of FieldGetter.
         */
        public static final FieldGetter INSTANCE = new FieldGetter();

        /**
         * The private field 'namesAndValues' in the {@link Metadata} class.
         */
        private UnsafeFieldAccessor headersField;

        /**
         * The private field 'nameBytes' in the {@link Key} class.
         */
        private UnsafeFieldAccessor namesField;

        private UnsafeFieldAccessor sizeField;

        /**
         * Constructs a new FieldGetter instance and initializes the private fields.
         * If any field is not found or an exception occurs, it catches the exception and ignores it.
         */
        FieldGetter() {
            try {
                Class<Metadata> type = Metadata.class;
                headersField = UnsafeFieldAccessorFactory.getAccessor(type, "namesAndValues");
                sizeField = UnsafeFieldAccessorFactory.getAccessor(type, "size");
                namesField = UnsafeFieldAccessorFactory.getAccessor(Key.class, ("nameBytes"));
            } catch (Throwable ignored) {
                // Ignore the exception if the field is not found or an error occurs
            }
        }

        /**
         * Retrieves the array of names and values from the given {@link Metadata} instance.
         * If the metadata is null or an exception occurs, it returns an empty array.
         *
         * @param metadata the Metadata instance from which to retrieve the names and values
         * @return the array of names and values, or an empty array if an error occurs
         */
        public Object[] getNamesAndValues(Metadata metadata) {
            if (metadata == null || headersField == null) {
                return new Object[0];
            }
            try {
                return (Object[]) headersField.get(metadata);
            } catch (Throwable e) {
                return new Object[0];
            }
        }

        public int getSize(Metadata metadata) {
            if (metadata == null || sizeField == null) {
                return 0;
            }
            try {
                return (int) sizeField.getInt(metadata);
            } catch (Throwable e) {
                return 0;
            }
        }

        /**
         * Retrieves the byte array representing the name from the given key.
         * If the key is null or an exception occurs, it returns an empty byte array.
         *
         * @param key the key from which to retrieve the name bytes
         * @return the byte array representing the name, or an empty byte array if an error occurs
         */
        public byte[] getNameBytes(Key<?> key) {
            if (key == null || namesField == null) {
                return new byte[0];
            }
            try {
                return (byte[]) namesField.get(key);
            } catch (Throwable e) {
                return new byte[0];
            }
        }
    }
}
