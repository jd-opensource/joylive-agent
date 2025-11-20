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
package com.jd.live.agent.governance.mcp.spec.v2;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.mcp.McpToolDefinitions;
import com.jd.live.agent.governance.mcp.McpVersion;
import com.jd.live.agent.governance.mcp.spec.JsonSchema;
import com.jd.live.agent.governance.mcp.spec.JsonSchema.JsonSchemaRef;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Extension("v2")
public class McpVersion2 implements McpVersion {

    @Override
    public String getRevision() {
        return "v2";
    }

    @Override
    public McpToolDefinitions createDefinitions() {
        return new McpToolDefinitionsV2();
    }

    @Override
    public JsonSchema output(JsonSchema schema) {
        return schema;
    }

    @Override
    public Object output(Object result) {
        return result;
    }

    /**
     * A container for managing and reusing JSON schemas across multiple types.
     */
    private static class McpToolDefinitionsV2 implements McpToolDefinitions {

        /**
         * Maps Java classes to their corresponding JSON schemas.
         */
        private final Map<Object, JsonSchemaRef> schemas = new HashMap<>();

        @Override
        public <K> JsonSchemaRef create(K key, Function<K, String> nameFunc, Function<K, JsonSchema> schemaFunc) {
            JsonSchemaRef result = schemas.computeIfAbsent(key, c -> {
                String name = nameFunc.apply(key);
                return new JsonSchemaRef(name, schemaFunc.apply(key), "#/$defs/" + name);
            });
            return result.hasReference() ? new JsonSchemaRef(result) : result;
        }

        @Override
        public Map<String, JsonSchema> getDefinitions() {
            Map<String, JsonSchema> result = new HashMap<>(schemas.size());
            for (Map.Entry<Object, JsonSchemaRef> entry : schemas.entrySet()) {
                JsonSchemaRef ref = entry.getValue();
                if (ref.getReference() > 1) {
                    result.put(ref.getName(), ref.getSchema());
                }
            }
            return result.isEmpty() ? null : result;
        }
    }
}
