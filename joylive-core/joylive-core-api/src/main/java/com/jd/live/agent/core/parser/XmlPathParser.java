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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Defines the contract for parsers that can extract data from XML documents based on XmlPath expressions.
 * This interface supports extensibility, allowing implementations to handle different XML parsing libraries
 * or strategies.
 */
@Extensible("XmlPathParser")
public interface XmlPathParser {

    int ORDER_JDK = 100;

    int ORDER_JXPATH = ORDER_JDK - 10;

    /**
     * Reads and extracts data from a XML document based on the specified XmlPath expression.
     * The method is generic, enabling it to return data of any type as specified by the caller.
     *
     * @param reader The reader from which the XML document is read.
     * @param path   The XmlPath expression used to extract data from the XML document.
     * @return The extracted data.
     */
    default String read(String reader, String path) {
        return reader == null || reader.isEmpty() ? null : read(new ByteArrayInputStream(reader.getBytes(StandardCharsets.UTF_8)), path);
    }

    /**
     * Reads and extracts data from a it document based on the specified XmlPath expression.
     * The method is generic, enabling it to return data of any type as specified by the caller.
     *
     * @param in   The InputStream from which the XML document is read.
     * @param path The XmlPath expression used to extract data from the XML document.
     * @return The extracted data.
     */
    String read(InputStream in, String path);

    /**
     * Validates that a path string contains only safe, well-defined characters.
     * <p>
     * A path is considered invalid if it:
     * <ul>
     *   <li>Is null or empty</li>
     *   <li>Contains undefined Unicode characters</li>
     *   <li>Contains single/double quotes (', ")</li>
     *   <li>Contains high/low surrogate pairs</li>
     *   <li>Contains ISO control characters</li>
     * </ul>
     *
     * @param path the path string to validate (may be null)
     * @return true if the path is non-null, non-empty, and contains only permitted characters,
     *         false otherwise
     * @see Character#isDefined(char)
     * @see Character#isISOControl(char)
     */
    default boolean validate(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        int length = path.length();
        for (int i = 0; i < length; ++i) {
            char ch = path.charAt(i);
            if (!Character.isDefined(ch)
                    || ch == '\''
                    || ch == '"'
                    || Character.isHighSurrogate(ch)
                    || Character.isISOControl(ch)
                    || Character.isLowSurrogate(ch)) {
                return false;
            }
        }
        return true;
    }

}
