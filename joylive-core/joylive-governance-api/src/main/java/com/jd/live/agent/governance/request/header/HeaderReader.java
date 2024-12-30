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
import java.util.List;
import java.util.function.Function;

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
    Iterator<String> getHeaderNames();

    /**
     * Returns a list of all values for the specified header key.
     *
     * @param key The key of the header.
     * @return A list of values for the specified header key.
     */
    List<String> getHeaders(String key);

    /**
     * Returns the first value for the specified header key.
     *
     * @param key The key of the header.
     * @return The first value for the specified header key, or null if the header is not present.
     */
    default String getHeader(String key) {
        List<String> values = getHeaders(key);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

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
}
