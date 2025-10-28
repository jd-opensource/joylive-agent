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
package com.jd.live.agent.core.parser;

import com.jd.live.agent.core.extension.annotation.Extensible;

import java.io.InputStream;

/**
 * Parser for extracting data from JSON documents using JSONPath expressions.
 *
 * @since 1.0.0
 */
@Extensible("JsonPathParser")
public interface JsonPathParser {

    /**
     * Reads data from JSON string using JSONPath.
     *
     * @param <T>    return type
     * @param reader JSON string
     * @param path   JSONPath expression
     * @return extracted data
     */
    <T> T read(String reader, String path);

    /**
     * Reads data from JSON string using JSONPath with default value.
     *
     * @param <T>          return type
     * @param reader       JSON string
     * @param path         JSONPath expression
     * @param defaultValue default value if extraction fails
     * @return extracted data or default value
     */
    default <T> T read(String reader, String path, T defaultValue) {
        try {
            T result = read(reader, path);
            return result == null ? defaultValue : result;
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    /**
     * Reads data from JSON InputStream using JSONPath.
     *
     * @param <T>  return type
     * @param in   JSON InputStream
     * @param path JSONPath expression
     * @return extracted data
     */
    <T> T read(InputStream in, String path);

    /**
     * Reads data from JSON InputStream using JSONPath with default value.
     *
     * @param <T>          return type
     * @param in           JSON InputStream
     * @param path         JSONPath expression
     * @param defaultValue default value if extraction fails
     * @return extracted data or default value
     */
    default <T> T read(InputStream in, String path, T defaultValue) {
        try {
            T result = read(in, path);
            return result == null ? defaultValue : result;
        } catch (Throwable e) {
            return defaultValue;
        }
    }
}
