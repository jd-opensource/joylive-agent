/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jd.live.agent.governance.openapi.media;

import com.jd.live.agent.core.parser.json.JsonField;
import com.jd.live.agent.governance.openapi.ExternalDocumentation;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Schema object that describes data types and structures for API parameters and responses.
 * Provides detailed information about data format, validation rules, and relationships.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schema {

    /**
     * Name of the schema
     */
    private String name;

    /**
     * Display title for the schema
     */
    private String title;

    /**
     * Human-readable description
     */
    private String description;

    /**
     * Data type (e.g., string, integer, object)
     */
    private String type;

    /**
     * Data format (e.g., int32, date-time)
     */
    private String format;

    /**
     * Default value for this schema
     */
    private Object defaultValue;

    /**
     * Schema properties if type is object
     */
    private Map<String, Schema> properties;

    /**
     * Items schema if type is array
     */
    private Schema items;

    /**
     * Reference to another schema definition
     */
    @JsonField("$ref")
    private String ref;

    /**
     * List of required property names
     */
    private List<String> required;

    /**
     * Indicates if null is allowed
     */
    private Boolean nullable;

    /**
     * Indicates if property is read-only
     */
    private Boolean readOnly;

    /**
     * Indicates if property is write-only
     */
    private Boolean writeOnly;

    /**
     * Indicates if schema is deprecated
     */
    private Boolean deprecated;

    /**
     * External documentation
     */
    private ExternalDocumentation externalDocs;

    /**
     * Additional properties for schemas that allow them
     */
    private Object additionalProperties;

    /**
     * Vendor extensions
     */
    private Map<String, Object> extensions;
}