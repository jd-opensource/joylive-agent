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
import java.util.Map;

/**
 * Defines the contract for configuration parsers that can parse configuration data from various sources
 * and formats into a map of strings to objects. This interface supports extensibility, allowing
 * implementations to support different configuration formats.
 *
 * @since 1.0.0
 */
@Extensible("ConfigParser")
public interface ConfigParser {

    /**
     * Represents the identifier for properties format.
     */
    String PROPERTIES = "properties";

    /**
     * Parses configuration data from the provided {@link Reader} and returns it as a map where each
     * key is a string representing the configuration key, and the value is the associated object.
     *
     * @param reader The reader from which configuration data is read.
     * @return A map representing the parsed configuration data.
     */
    Map<String, Object> parse(Reader reader);

    default boolean isFlatted() {
        return false;
    }
}
