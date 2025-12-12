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
package com.jd.live.agent.core.mcp.spec.v1;

import com.jd.live.agent.core.parser.annotation.JsonField;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * A JSON Schema object that describes the expected structure of arguments or output.
 */
@Getter
@Setter
public class JsonSchema implements Serializable {

    /**
     * The URI reference to another schema.
     * <p>
     * Format examples:
     * <ul>
     *   <li>#/$defs/address - Internal reference</li>
     *   <li>https://example.com/schemas/address.json - External reference</li>
     * </ul>
     * </p>
     */
    @JsonField("$schema")
    private String ref;

    /**
     * The type of the schema (e.g., "object")
     */
    private String type;

    private String in;

    /**
     * Short title describing the schema's purpose, typically used for display in documentation.
     */
    private String title;

    /**
     * The description the schema
     */
    private String description;

    /**
     * Maximum length constraint when type is "string".
     */
    private Integer maxLength;

    /**
     * Minimum length constraint when type is "string".
     */
    private Integer minLength;

    /**
     * Semantic format of the data when type is "string". includes "email" | "uri" | "date" | "date-time"
     */
    private String format;

    @JsonField("const")
    private String constValue;

    /**
     * Default value to be used when instance value is undefined.
     */
    @JsonField("default")
    private Object defaultValue;

    /**
     * Minimum allowed numeric value when type is "number" or "integer"
     */
    private BigDecimal minimum;

    /**
     * Maximum allowed numeric value when type is "number" or "integer"
     */
    private BigDecimal maximum;

    /**
     * Array of enum values to choose from.
     * for single-selection enumeration without display titles for options when type is "string".
     */
    @JsonField("enum")
    private List<String> enums;
    /**
     * (Legacy) Display names for enum values.
     * Non-standard according to JSON schema 2020-12.
     */
    private List<String> enumNames;

    /**
     * Array of enum options with values and display labels when type is "string".
     * The const of each schema is the enum value.
     * The title of each schema is the display label for this option
     */
    private List<JsonSchema> oneOf;

    /**
     * Maximum number of items allowed in array instances when type is "array"
     */
    private Integer maxItems;

    /**
     * Minimum number of items required in array instances when type is "array"
     */
    private Integer minItems;

    /**
     * The array item schema for multiple-selection enumeration without display titles when type is "array".
     * <li>1. The type of items is "string" and the enum of items is array of enum values to choose from
     * <li>2. The type of items is "array" and the anyOf of items is array of enum options with values and display labels
     */
    private JsonSchema items;

    /**
     * Array of enum options with values and display labels when type is "array".
     */
    private List<JsonSchema> anyOf;

    /**
     * The properties of the schema object
     */
    private Map<String, JsonSchema> properties;

    /**
     * List of required property names
     */
    private List<String> required;

    /**
     * Whether additional properties are allowed
     */
    private Boolean additionalProperties;

    public JsonSchema() {
    }

    public JsonSchema(String ref) {
        this.ref = ref;
    }

    @Builder
    public JsonSchema(String type,
                      String in,
                      String title,
                      String description,
                      Integer maxLength,
                      Integer minLength,
                      String format,
                      String constValue,
                      Object defaultValue,
                      BigDecimal minimum,
                      BigDecimal maximum,
                      List<String> enums,
                      List<String> enumNames,
                      List<JsonSchema> oneOf,
                      Integer maxItems,
                      Integer minItems,
                      JsonSchema items,
                      List<JsonSchema> anyOf,
                      Map<String, JsonSchema> properties,
                      List<String> required,
                      Boolean additionalProperties) {
        this(null, type, in, title, description, maxLength, minLength, format, constValue, defaultValue, minimum, maximum,
                enums, enumNames, oneOf, maxItems, minItems, items, anyOf, properties, required, additionalProperties);
    }

    private JsonSchema(String ref,
                       String type,
                       String in,
                       String title,
                       String description,
                       Integer maxLength,
                       Integer minLength,
                       String format,
                       String constValue,
                       Object defaultValue,
                       BigDecimal minimum,
                       BigDecimal maximum,
                       List<String> enums,
                       List<String> enumNames,
                       List<JsonSchema> oneOf,
                       Integer maxItems,
                       Integer minItems,
                       JsonSchema items,
                       List<JsonSchema> anyOf,
                       Map<String, JsonSchema> properties,
                       List<String> required,
                       Boolean additionalProperties) {
        this.ref = ref;
        this.type = type;
        this.in = in;
        this.title = title;
        this.description = description;
        this.maxLength = maxLength;
        this.minLength = minLength;
        this.format = format;
        this.constValue = constValue;
        this.defaultValue = defaultValue;
        this.minimum = minimum;
        this.maximum = maximum;
        this.enums = enums;
        this.enumNames = enumNames;
        this.oneOf = oneOf;
        this.maxItems = maxItems;
        this.minItems = minItems;
        this.items = items;
        this.anyOf = anyOf;
        this.properties = properties;
        this.required = required;
        this.additionalProperties = additionalProperties;
    }

    public JsonSchema clone() {
        return new JsonSchema(ref, type, in, title, description, maxLength, minLength, format, constValue, defaultValue,
                minimum, maximum, enums, enumNames, oneOf, maxItems, minItems, items, anyOf,
                properties, required, additionalProperties);
    }
}
