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
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A JSON Schema object that describes the expected structure of arguments or output.
 */
@Getter
@Setter
@NoArgsConstructor
public class JsonSchema {

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
    @JsonField("$ref")
    private String ref;

    /**
     * The type of the schema (e.g., "object")
     */
    private String type;

    /**
     * The description the schema
     */
    private String description;

    /**
     * The properties of the schema object
     */
    private Map<String, JsonSchema> properties;
    /**
     * Defines the schema for array items when type is "array".
     * For homogeneous arrays, this is a single schema that all items must satisfy.
     * For tuple validation (heterogeneous arrays), this can be a list of schemas
     * where each item must match the schema at the same position.
     */
    private JsonSchema items;
    /**
     * List of required property names
     */
    private List<String> required;
    /**
     * Whether additional properties are allowed
     */
    private Boolean additionalProperties;

    public JsonSchema(String ref) {
        this.ref = ref;
    }

    @Builder
    public JsonSchema(String type,
                      String description,
                      Map<String, JsonSchema> properties,
                      JsonSchema items,
                      List<String> required,
                      Boolean additionalProperties) {
        this(null, type, description, properties, items, required, additionalProperties);
    }

    private JsonSchema(String ref,
                       String type,
                       String description,
                       Map<String, JsonSchema> properties,
                       JsonSchema items,
                       List<String> required,
                       Boolean additionalProperties) {
        this.ref = ref;
        this.description = description;
        this.type = type;
        this.properties = properties;
        this.items = items;
        this.required = required;
        this.additionalProperties = additionalProperties;
    }

    public JsonSchema clone() {
        try {
            return (JsonSchema) super.clone();
        } catch (CloneNotSupportedException e) {
            return new JsonSchema(ref, type, properties, items, required, additionalProperties);
        }
    }

    /**
     * Represents a reference to a JSON schema with reference counting.
     * Used to manage schema references and convert schemas to reference format.
     */
    public static class JsonSchemaRef {

        @Getter
        private String name;

        @Getter
        @Setter
        private JsonSchema schema;

        @Getter
        private String uri;

        private AtomicInteger references;

        public JsonSchemaRef(String name, JsonSchema schema, String uri) {
            this(name, schema, uri, new AtomicInteger(0));
        }

        public JsonSchemaRef(JsonSchemaRef ref) {
            this(ref.name, ref.schema == null ? null : ref.schema.clone(), ref.uri, ref.references);
        }

        private JsonSchemaRef(String name, JsonSchema schema, String uri, AtomicInteger references) {
            this.name = name;
            this.schema = schema;
            this.uri = uri;
            this.references = references;
        }

        /**
         * Increments and returns the reference count.
         *
         * @return the updated reference count
         */
        public int getAndIncReference() {
            return references.getAndIncrement();
        }

        public int getReference() {
            return references.get();
        }

        public JsonSchemaRef ref() {
            return new JsonSchemaRef(name, new JsonSchema(uri), uri, references);
        }

        public boolean hasReference() {
            return references.get() > 0;
        }
    }
}
