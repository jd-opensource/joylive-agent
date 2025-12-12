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
import com.jd.live.agent.core.openapi.spec.v3.annotation.OpenApi31;
import com.jd.live.agent.core.openapi.spec.v3.annotation.Validation;
import com.jd.live.agent.core.parser.annotation.JsonField;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a JSON Schema document that defines the structure, validation rules, and semantic meaning of JSON data.
 * A Schema can be used to validate instances, provide metadata, define UI generation hints, or specify hyperlink navigation.
 * Based on JSON Schema specification draft-bhutton-json-schema-00.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schema {

    /**
     * The canonical URI for this schema resource, used for schema identification and reference resolution.
     * Corresponds to the "$id" keyword in JSON Schema.
     */
    @JsonField("$id")
    @OpenApi31
    private String id;

    /**
     * URI reference to another schema to be used in place of this schema.
     * Corresponds to the "$ref" keyword in JSON Schema.
     */
    @JsonField("$ref")
    private String ref;

    /**
     * Name identifier for the schema, used for identification purposes.
     */
    private String name;

    /**
     * Short title describing the schema's purpose, typically used for display in documentation.
     */
    private String title;

    /**
     * Non-executable comments intended for schema authors and maintainers.
     * Corresponds to the "$comment" keyword in JSON Schema.
     */
    @JsonField("$comment")
    @OpenApi31
    private String comment;

    /**
     * Human-readable explanation of the schema's purpose, validation behavior, and usage.
     */
    private String description;

    /**
     * Links to external documentation providing additional information about the schema.
     */
    private ExternalDoc externalDocs;

    /**
     * Primary data type for validation, one of: "null", "boolean", "object", "array", "number", "string", or "integer".
     */
    private String type;

    /**
     * Multiple allowed data types when an instance can be valid against more than one type.
     */
    @OpenApi31
    private Set<String> types;

    /**
     * Semantic format of the data, providing additional validation constraints beyond the basic type.
     * Common formats include: "date-time", "email", "hostname", "ipv4", "ipv6", "uri", etc.
     */
    private String format;

    /**
     * Default value to be used when instance value is undefined.
     */
    @JsonField("default")
    private Object defaultValue;

    /**
     * Value for boolean schemas (true/false) that always produce the same validation result regardless of instance.
     */
    private Boolean booleanSchemaValue;

    /**
     * Map of property names to schemas, defining validation rules for specific object properties.
     */
    @Singular(ignoreNullCollections = true)
    private Map<String, Schema> properties;

    /**
     * Map of regular expression patterns to schemas, for validating dynamically named properties.
     */
    @OpenApi31
    private Map<String, Schema> patternProperties;

    /**
     * Schema applied to all items in an array instance.
     */
    private Schema items;

    /**
     * Numeric validation requiring instance to be a multiple of this value.
     */
    @Validation
    private BigDecimal multipleOf;

    /**
     * Maximum allowed numeric value (inclusive by default).
     */
    @Validation
    private BigDecimal maximum;

    /**
     * When true, maximum value becomes exclusive rather than inclusive.
     */
    @Validation
    private Boolean exclusiveMaximum;

    /**
     * Explicit exclusive maximum value for numeric validation.
     */
    @Validation
    @OpenApi31
    private BigDecimal exclusiveMaximumValue;

    /**
     * Minimum allowed numeric value (inclusive by default).
     */
    @Validation
    private BigDecimal minimum;

    /**
     * When true, minimum value becomes exclusive rather than inclusive.
     */
    @Validation
    private Boolean exclusiveMinimum;

    /**
     * Explicit exclusive minimum value for numeric validation.
     */
    @Validation
    @OpenApi31
    private BigDecimal exclusiveMinimumValue;

    /**
     * Maximum length constraint for string instances.
     */
    @Validation
    private Integer maxLength;

    /**
     * Minimum length constraint for string instances.
     */
    @Validation
    private Integer minLength;

    /**
     * Regular expression pattern that string instances must match.
     */
    @Validation
    private String pattern;

    /**
     * Maximum number of items allowed in array instances.
     */
    @Validation
    private Integer maxItems;

    /**
     * Minimum number of items required in array instances.
     */
    @Validation
    private Integer minItems;

    /**
     * Maximum number of array items that must match the 'contains' schema.
     */
    @Validation
    @OpenApi31
    private Integer maxContains;

    /**
     * Minimum number of array items that must match the 'contains' schema.
     */
    @Validation
    @OpenApi31
    private Integer minContains;

    /**
     * When true, requires all array items to be unique.
     */
    @Validation
    private Boolean uniqueItems;

    /**
     * Maximum number of properties allowed in object instances.
     */
    @Validation
    private Integer maxProperties;

    /**
     * Minimum number of properties required in object instances.
     */
    @Validation
    private Integer minProperties;

    /**
     * List of property names that must be present in object instances.
     */
    @Validation
    @Singular(value = "required", ignoreNullCollections = true)
    private List<String> required;

    /**
     * When true, allows null as a valid value in addition to the specified type.
     */
    @Validation
    private Boolean nullable;

    /**
     * When true, indicates property is intended for reading only and should not be modified.
     */
    @Validation
    private Boolean readOnly;

    /**
     * When true, indicates property is intended for writing only and will not be returned in responses.
     */
    @Validation
    private Boolean writeOnly;

    /**
     * When true, indicates the schema is deprecated and usage should be avoided.
     */
    private Boolean deprecated;

    /**
     * Schema for validating additional properties not specifically defined in 'properties' or 'patternProperties'.
     * When false, no additional properties are allowed. When true or omitted, any additional properties are allowed.
     */
    private Object additionalProperties;

    /**
     * Schema for validating additional array items beyond those defined by prefixItems.
     */
    @Validation
    @OpenApi31
    private Schema additionalItems;

    /**
     * Schema for validating array items not evaluated by other keywords like 'items' or 'prefixItems'.
     */
    @Validation
    @OpenApi31
    private Schema unevaluatedItems;

    /**
     * Schema for validating object properties not evaluated by other keywords like 'properties' or 'patternProperties'.
     */
    @Validation
    @OpenApi31
    private Schema unevaluatedProperties;

    /**
     * List of allowed values for the instance. Instance must equal one of these values.
     */
    @Validation
    @JsonField("enum")
    private List<? extends Object> enums;

    /**
     * Single value that instance must equal exactly.
     */
    @Validation
    @OpenApi31
    @JsonField("_const")
    private Object constValue;

    /**
     * Object used to support polymorphic type handling through a discriminator property.
     */
    private Discriminator discriminator;

    /**
     * Sample value demonstrating a valid instance according to this schema.
     */
    protected Object example;

    /**
     * Multiple sample values demonstrating valid instances according to this schema.
     */
    @OpenApi31
    private List<Object> examples;

    /**
     * Configuration for XML serialization of the schema instance.
     */
    private XML xml;

    /**
     * List of schemas for validating items at specific positions in tuple validation arrays.
     */
    @OpenApi31
    private List<Schema> prefixItems;

    /**
     * List of schemas that instance must validate against all of (logical AND).
     */
    @Validation
    private List<Schema> allOf;

    /**
     * List of schemas where instance must validate against at least one (logical OR).
     */
    @Validation
    private List<Schema> anyOf;

    /**
     * List of schemas where instance must validate against exactly one (logical XOR).
     */
    @Validation
    private List<Schema> oneOf;

    /**
     * Schema that instance must not validate against (logical NOT).
     */
    @Validation
    private Schema not;

    /**
     * Schema that at least one array item must validate against.
     */
    @Validation
    @OpenApi31
    private Schema contains;

    /**
     * URI identifying the JSON Schema dialect this schema uses.
     * Corresponds to the "$schema" keyword in JSON Schema.
     */
    @OpenApi31
    @JsonField("$schema")
    private String schema;

    /**
     * Defines a location-independent identifier for the schema using a plain name fragment.
     * Corresponds to the "$anchor" keyword in JSON Schema.
     */
    @OpenApi31
    @JsonField("$anchor")
    private String anchor;

    /**
     * Declares which vocabularies are used in this schema.
     * Corresponds to the "$vocabulary" keyword in JSON Schema.
     */
    @OpenApi31
    @JsonField("$vocabulary")
    private String vocabulary;

    /**
     * Defines a dynamic anchor for recursive schema references.
     * Corresponds to the "$dynamicAnchor" keyword in JSON Schema.
     */
    @OpenApi31
    @JsonField("$dynamicAnchor")
    private String dynamicAnchor;

    /**
     * Reference that gets dynamically resolved at runtime based on dynamic scope.
     * Corresponds to the "$dynamicRef" keyword in JSON Schema.
     */
    @OpenApi31
    @JsonField("$dynamicRef")
    private String dynamicRef;

    /**
     * Encoding format for string instance content (e.g., "base64", "base16").
     */
    @OpenApi31
    private String contentEncoding;

    /**
     * Media type of string instance content (e.g., "application/json", "text/html").
     */
    @OpenApi31
    private String contentMediaType;

    /**
     * Schema to validate decoded content after applying contentEncoding.
     */
    @Validation
    @OpenApi31
    private Schema contentSchema;

    /**
     * Schema applied to validate all property names in an object instance.
     * Unlike "properties" which validates values, this validates the property names themselves.
     * Property names must be valid against this schema to be accepted.
     */
    @Validation
    @OpenApi31
    private Schema propertyNames;

    /**
     * Schema for conditional validation - defines the condition to test.
     */
    @Validation
    @OpenApi31
    @JsonField("_if")
    private Schema ifCnd;

    /**
     * Schema for conditional validation - applied when 'if' condition fails.
     */
    @Validation
    @OpenApi31
    @JsonField("_else")
    private Schema elseCnd;

    /**
     * Schema for conditional validation - applied when 'if' condition passes.
     */
    @Validation
    @OpenApi31
    @JsonField("then")
    private Schema thenCnd;

    /**
     * Conditional validation based on the presence of specific properties.
     * Maps property names to schemas that are only evaluated when the specified property exists in the instance.
     * For example, if a "credit_card" property exists, additional validation rules might apply to the entire object.
     */
    @Validation
    @OpenApi31
    private Map<String, Schema> dependentSchemas;

    /**
     * Conditional property requirements based on the presence of specific properties.
     * Maps property names to lists of other property names that become required when the key property exists.
     * For example, if "credit_card" property exists, then "billing_address" might become required.
     */
    @Validation
    @OpenApi31
    private Map<String, List<String>> dependentRequired;

    /**
     * Custom extensions for schema metadata not covered by standard keywords.
     */
    private Map<String, Object> extensions;

}