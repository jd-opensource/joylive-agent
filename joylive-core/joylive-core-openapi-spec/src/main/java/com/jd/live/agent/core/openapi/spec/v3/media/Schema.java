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
package com.jd.live.agent.core.openapi.spec.v3.media;

import com.jd.live.agent.core.openapi.spec.v3.ExternalDoc;
import com.jd.live.agent.core.parser.annotation.JsonField;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Schema object describing data types and validation rules for API parameters and responses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schema {

    /**
     * Reference to another schema definition using JSON Schema $ref format.
     */
    @JsonField("$ref")
    private String ref;

    /**
     * Name of the schema for identification and reference.
     */
    private String name;

    /**
     * Display title for the schema in documentation.
     */
    private String title;

    /**
     * Human-readable description explaining the schema's purpose.
     */
    private String description;

    /**
     * External documentation providing additional schema details.
     */
    private ExternalDoc externalDocs;

    /**
     * Data type defining the schema's basic structure (e.g., string, integer, object).
     */
    private String type;

    /**
     * Data format specifying detailed type constraints (e.g., int32, date-time, email).
     */
    private String format;

    /**
     * Default value used when no value is provided in requests or responses.
     */
    @JsonField("default")
    private Object defaultValue;

    /**
     * Boolean schema value for constant boolean schemas.
     */
    private Boolean booleanSchemaValue;

    /**
     * Schema properties defining object structure when type is "object".
     */
    @Singular(ignoreNullCollections = true)
    private Map<String, Schema> properties;

    /**
     * Items schema defining array element structure when type is "array".
     */
    private Schema items;

    /**
     * Numeric constraint requiring values to be multiples of this number.
     */
    private BigDecimal multipleOf;

    /**
     * Maximum numeric value constraint for validation.
     */
    private BigDecimal maximum;

    /**
     * Whether maximum value constraint is exclusive.
     */
    private Boolean exclusiveMaximum;

    /**
     * Minimum numeric value constraint for validation.
     */
    private BigDecimal minimum;

    /**
     * Whether minimum value constraint is exclusive.
     */
    private Boolean exclusiveMinimum;

    /**
     * Maximum string length constraint for validation.
     */
    private Integer maxLength;

    /**
     * Minimum string length constraint for validation.
     */
    private Integer minLength;

    /**
     * Regular expression pattern constraint for string validation.
     */
    private String pattern;

    /**
     * Maximum number of items constraint for array validation.
     */
    private Integer maxItems;

    /**
     * Minimum number of items constraint for array validation.
     */
    private Integer minItems;

    /**
     * Whether array items must be unique.
     */
    private Boolean uniqueItems;

    /**
     * Maximum number of properties constraint for object validation.
     */
    private Integer maxProperties;

    /**
     * Minimum number of properties constraint for object validation.
     */
    private Integer minProperties;

    /**
     * List of required property names for object validation.
     */
    @Singular(value = "required", ignoreNullCollections = true)
    private List<String> required;

    /**
     * Indicates if null values are allowed for this schema.
     */
    private Boolean nullable;

    /**
     * Indicates if property is read-only.
     */
    private Boolean readOnly;

    /**
     * Indicates if property is write-only.
     */
    private Boolean writeOnly;

    /**
     * Indicates if this schema definition is deprecated.
     */
    private Boolean deprecated;

    /**
     * Additional properties schema for objects that allow extra properties.
     */
    private Object additionalProperties;

    private Discriminator discriminator;

    /**
     * Example value demonstrating valid schema data.
     */
    protected Object example;

    /**
     * List of example values showing various valid data formats.
     */
    private List<Object> examples;

    /**
     * List of valid enumeration values for string schemas.
     */
    @JsonField("enum")
    private List<? extends Object> enums;

    private XML xml;

    /**
     * Schema for prefix items in tuple validation arrays.
     */
    private List<Schema> prefixItems;

    /**
     * List of schemas that must all be satisfied (AND logic).
     */
    private List<Schema> allOf;

    /**
     * List of schemas where at least one must be satisfied (OR logic).
     */
    private List<Schema> anyOf;

    /**
     * List of schemas where exactly one must be satisfied (XOR logic).
     */
    private List<Schema> oneOf;

    /**
     * Vendor extensions for additional schema metadata.
     */
    private Map<String, Object> extensions;

}