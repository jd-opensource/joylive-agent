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
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Interface for parsing schema information from classes for JSON serialization.
 * Implementations scan classes to extract field metadata suitable for JSON schema generation.
 */
@Extensible("JsonSchemaParser")
public interface JsonSchemaParser {

    /**
     * Scans a class and returns a list of fields that can be used for JSON schema generation.
     *
     * @param cls The class to scan for JSON-compatible fields
     * @return List of field schemas containing metadata about each field
     */
    List<FieldSchema> describe(Class<?> cls);

    /**
     * Represents metadata about a field in the context of JSON schema generation.
     */
    @Getter
    @AllArgsConstructor
    class FieldSchema {
        private final String name;
        private final Field field;
        private final AccessType accessType;
    }

    /**
     * Defines access types for properties in data serialization/deserialization.
     */
    enum AccessType {
        /**
         * Read-only access (serialization only)
         */
        READ,

        /**
         * Write-only access (deserialization only)
         */
        WRITE,

        /**
         * Full access (both serialization and deserialization)
         */
        READ_WRITE
    }

}
