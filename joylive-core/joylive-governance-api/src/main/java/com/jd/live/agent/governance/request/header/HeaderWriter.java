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

import java.util.List;

/**
 * Interface for writing HTTP headers.
 * <p>
 * This interface defines a method to set a header with a specified key and value.
 */
public interface HeaderWriter {

    /**
     * Returns the value for the specified header key.
     *
     * @param key The key of the header.
     * @return The value for the specified header key, or null if the header is not present.
     */
    String getHeader(String key);

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
}