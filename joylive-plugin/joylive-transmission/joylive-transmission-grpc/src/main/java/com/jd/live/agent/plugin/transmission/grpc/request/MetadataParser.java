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

import com.jd.live.agent.governance.request.HeaderParser;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    public boolean isDuplicable() {
        return true;
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
        Key<String> metaKey = getOrCreate(key);

        byte[] bytes = FieldGetter.INSTANCE.getNameBytes(metaKey);

        int last = -1;
        int index;
        int counter = 0;
        // update kv
        int size = FieldGetter.INSTANCE.getSize(metadata);
        Object[] namesAndValues = FieldGetter.INSTANCE.getNamesAndValues(metadata);
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                index = i * 2;
                if (Arrays.equals((byte[]) namesAndValues[index], bytes)) {
                    counter++;
                    last = index;
                }
            }
        }
        if (counter == 0) {
            // add
            metadata.put(metaKey, value);
        } else if (counter == 1) {
            // most is only one
            namesAndValues[last + 1] = ASCII_STRING_MARSHALLER.toAsciiString(value).getBytes(StandardCharsets.US_ASCII);
        } else {
            metadata.removeAll(metaKey);
            // always add
            metadata.put(metaKey, value);
        }
    }

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
        private Field headersField;

        /**
         * The private field 'nameBytes' in the {@link Key} class.
         */
        private Field namesField;

        private Field sizeField;

        /**
         * Constructs a new FieldGetter instance and initializes the private fields.
         * If any field is not found or an exception occurs, it catches the exception and ignores it.
         */
        FieldGetter() {
            try {
                headersField = Metadata.class.getDeclaredField("namesAndValues");
                headersField.setAccessible(true);
                sizeField = Metadata.class.getDeclaredField("size");
                sizeField.setAccessible(true);
                namesField = Key.class.getDeclaredField("nameBytes");
                namesField.setAccessible(true);
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
                return (int) sizeField.get(metadata);
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
