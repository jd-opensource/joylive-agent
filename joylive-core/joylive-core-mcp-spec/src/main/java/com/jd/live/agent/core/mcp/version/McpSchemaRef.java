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
package com.jd.live.agent.core.mcp.version;

import com.jd.live.agent.core.mcp.spec.v1.JsonSchema;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a reference to a JSON schema with reference counting.
 * Used to manage schema references and convert schemas to reference format.
 */
public class McpSchemaRef {

    @Getter
    private String name;

    @Getter
    @Setter
    private JsonSchema schema;

    @Getter
    private String uri;

    private AtomicInteger references;

    public McpSchemaRef(String name, JsonSchema schema, String uri) {
        this(name, schema, uri, new AtomicInteger(0));
    }

    public McpSchemaRef(McpSchemaRef ref) {
        this(ref.name, ref.schema == null ? null : ref.schema.clone(), ref.uri, ref.references);
    }

    private McpSchemaRef(String name, JsonSchema schema, String uri, AtomicInteger references) {
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

    public McpSchemaRef ref() {
        return new McpSchemaRef(name, new JsonSchema(uri), uri, references);
    }

    public boolean hasReference() {
        return references.get() > 0;
    }
}
