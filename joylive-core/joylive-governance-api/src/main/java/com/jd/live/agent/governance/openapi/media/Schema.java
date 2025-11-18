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
import com.jd.live.agent.governance.openapi.ExternalDoc;
import lombok.*;

import java.math.BigDecimal;
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
     * Reference to another schema definition
     */
    @JsonField("$ref")
    private String ref;

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
     * External documentation
     */
    private ExternalDoc externalDocs;

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
     * when set, this represents a boolean schema value
     */
    private Boolean booleanSchemaValue;

    /**
     * Schema properties if type is object
     */
    @Singular(ignoreNullCollections = true)
    private Map<String, Schema> properties;

    /**
     * Items schema if type is array
     */
    private Schema items;

    /**
     * Numeric value must be a multiple of this value
     */
    private BigDecimal multipleOf;

    /**
     * Maximum value constraint
     */
    private BigDecimal maximum;

    /**
     * Whether maximum value is exclusive
     */
    private Boolean exclusiveMaximum;

    /**
     * Minimum value constraint
     */
    private BigDecimal minimum;

    /**
     * Whether minimum value is exclusive
     */
    private Boolean exclusiveMinimum;

    /**
     * Maximum string length constraint
     */
    private Integer maxLength;

    /**
     * Minimum string length constraint
     */
    private Integer minLength;

    /**
     * Regular expression pattern constraint
     */
    private String pattern;

    /**
     * Maximum number of array items constraint
     */
    private Integer maxItems;

    /**
     * Minimum number of array items constraint
     */
    private Integer minItems;

    /**
     * Whether array items must be unique
     */
    private Boolean uniqueItems;

    /**
     * Maximum number of object properties constraint
     */
    private Integer maxProperties;

    /**
     * Minimum number of object properties constraint
     */
    private Integer minProperties;

    /**
     * List of required property names
     */
    @Singular(value = "required", ignoreNullCollections = true)
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
     * Additional properties for schemas that allow them
     */
    private Object additionalProperties;

    private Discriminator discriminator;

    protected Object example;

    private List<Object> examples;

    private List<String> enums;

    private List<Schema> prefixItems;
    private List<Schema> allOf;
    private List<Schema> anyOf;
    private List<Schema> oneOf;

    /**
     * Vendor extensions
     */
    private Map<String, Object> extensions;

}