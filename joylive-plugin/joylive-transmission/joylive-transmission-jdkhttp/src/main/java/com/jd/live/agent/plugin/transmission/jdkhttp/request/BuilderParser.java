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
package com.jd.live.agent.plugin.transmission.jdkhttp.request;

import com.jd.live.agent.governance.request.header.HeaderReader;
import com.jd.live.agent.governance.request.header.HeaderWriter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

public class BuilderParser implements HeaderReader, HeaderWriter {

    private final TreeMap<String, List<String>> headers;

    public BuilderParser(TreeMap<String, List<String>> headers) {
        this.headers = headers;
    }

    @Override
    public Iterator<String> getNames() {
        return headers.keySet().iterator();
    }

    @Override
    public List<String> getHeaders(String key) {
        return headers.get(key);
    }

    @Override
    public String getHeader(String key) {
        List<String> values = headers.get(key);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    @Override
    public void setHeader(String key, String value) {
        List<String> values = new ArrayList<>(1);
        values.add(value);
        headers.put(key, values);
    }

    /**
     * Creates a new instance of {@link BuilderParser} using the names and values from the given builder object.
     *
     * @param builder the builder object from which to extract the names and values
     * @return a new instance of BuilderParser initialized with the names and values
     */
    public static BuilderParser of(Object builder) {
        return new BuilderParser(FieldGetter.INSTANCE.getNamesAndValues(builder));
    }

    /**
     * A utility class to access private fields of the internal HTTP request and headers builder classes.
     */
    private static class FieldGetter {

        /**
         * The singleton instance of FieldGetter.
         */
        public static final FieldGetter INSTANCE = new FieldGetter();

        /**
         * The private field 'headersBuilder' in the internal HTTP request builder class.
         */
        private Field builderField;

        /**
         * The private field 'headersMap' in the internal headers builder class.
         */
        private Field headersField;

        /**
         * Constructs a new FieldGetter instance and initializes the private fields.
         * If any field is not found or an exception occurs, it catches the exception and ignores it.
         */
        FieldGetter() {
            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                Class<?> requestBuilderType = classLoader.loadClass("jdk.internal.net.http.HttpRequestBuilderImpl");
                Class<?> headersBuilderType = classLoader.loadClass("jdk.internal.net.http.common.HttpHeadersBuilder");
                builderField = requestBuilderType.getDeclaredField("headersBuilder");
                builderField.setAccessible(true);
                headersField = headersBuilderType.getDeclaredField("headersMap");
                headersField.setAccessible(true);
            } catch (Throwable ignored) {
                // Ignore the exception if the field is not found or an error occurs
            }
        }

        /**
         * Retrieves the map of names and values from the given builder object.
         * If the builder is null or an exception occurs, it returns an empty TreeMap.
         *
         * @param builder the builder object from which to retrieve the names and values
         * @return the map of names and values, or an empty TreeMap if an error occurs
         */
        @SuppressWarnings("unchecked")
        public TreeMap<String, List<String>> getNamesAndValues(Object builder) {
            if (builder == null || builderField == null || headersField == null) {
                return new TreeMap<>();
            }
            try {
                return (TreeMap<String, List<String>>) headersField.get(builderField.get(builder));
            } catch (Throwable e) {
                return new TreeMap<>();
            }
        }
    }

}
