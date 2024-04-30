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

import java.io.Reader;

/**
 * Defines the contract for parsers that can extract data from JSON documents based on JSONPath expressions.
 * This interface supports extensibility, allowing implementations to handle different JSON parsing libraries
 * or strategies.
 *
 * @since 1.0.0
 */
@Extensible("JsonPathParser")
public interface JsonPathParser {

    /**
     * Reads and extracts data from a JSON document based on the specified JSONPath expression.
     * The method is generic, enabling it to return data of any type as specified by the caller.
     *
     * @param <T>    The type of the data to be returned.
     * @param reader The reader from which the JSON document is read.
     * @param path   The JSONPath expression used to extract data from the JSON document.
     * @return The extracted data of type {@code T}.
     */
    <T> T read(Reader reader, String path);

}
