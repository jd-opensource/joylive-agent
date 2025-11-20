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
package com.jd.live.agent.governance.mcp;

import com.jd.live.agent.governance.mcp.spec.JsonSchema;
import com.jd.live.agent.governance.mcp.spec.JsonSchema.JsonSchemaRef;

import java.util.Map;
import java.util.function.Function;

/**
 * Interface for managing JSON Schema definitions with reference tracking.
 * Provides functionality to create, retrieve and organize schema definitions.
 */
public interface McpToolDefinitions {

    /**
     * Creates or retrieves a schema with the specified key.
     *
     * @param key The key to identify the schema
     * @param nameFunc Function to generate schema name
     * @param schemaFunc Function to create schema if not found
     * @return Schema reference with tracking information
     */
    <K> JsonSchemaRef create(K key, Function<K, String> nameFunc, Function<K, JsonSchema> schemaFunc);

    /**
     * Extracts reusable schema definitions based on reference counts.
     * <p>
     * Only schemas referenced multiple times are included in the
     * definitions map. Uses simple class names as keys when possible,
     * falling back to fully qualified names to resolve conflicts.
     *
     * @return a map of schema definitions, or null if no reusable schemas exist
     */
    Map<String, JsonSchema> getDefinitions();
}
