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
 * Defines the contract for parsers that can extract data from XML documents based on XmlPath expressions.
 * This interface supports extensibility, allowing implementations to handle different XML parsing libraries
 * or strategies.
 *
 * @since 1.0.0
 */
@Extensible("XmlPathParser")
public interface XmlPathParser {

    /**
     * Reads and extracts data from a XML document based on the specified XmlPath expression.
     * The method is generic, enabling it to return data of any type as specified by the caller.
     *
     * @param reader The reader from which the XML document is read.
     * @param path   The XmlPath expression used to extract data from the XML document.
     * @return The extracted data.
     */
    String read(String reader, String path);

    /**
     * Reads and extracts data from a it document based on the specified XmlPath expression.
     * The method is generic, enabling it to return data of any type as specified by the caller.
     *
     * @param in   The InputStream from which the XML document is read.
     * @param path The XmlPath expression used to extract data from the XML document.
     * @return The extracted data.
     */
    String read(InputStream in, String path);

}
