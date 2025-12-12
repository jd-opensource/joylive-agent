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
package com.jd.live.agent.core.mcp.version.v2;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.mcp.spec.v1.JsonSchema;
import com.jd.live.agent.core.mcp.version.McpSchemaRef;
import com.jd.live.agent.core.mcp.version.McpToolDefinitions;
import com.jd.live.agent.core.mcp.version.McpVersion;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.jd.live.agent.core.mcp.spec.v1.Tool.COMPONENT_REF_PREFIX;

@Extension("v2")
public class McpVersion2 implements McpVersion {

    public static final McpVersion INSTANCE = new McpVersion2();

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
        private final Map<Object, McpSchemaRef> schemas = new HashMap<>();

        @Override
        public <K> McpSchemaRef create(K key, Function<K, String> nameFunc, Function<K, JsonSchema> schemaFunc) {
            McpSchemaRef result = schemas.computeIfAbsent(key, c -> {
                String name = nameFunc.apply(key);
                String url = name == null || name.isEmpty() ? null : COMPONENT_REF_PREFIX + name;
                return new McpSchemaRef(name, null, url);
            });
            if (result.getAndIncReference() == 0) {
                result.setSchema(schemaFunc.apply(key));
                return result;
            }
            return result.ref();
        }

        @Override
        public Map<String, JsonSchema> getDefinitions() {
            Map<String, JsonSchema> result = new HashMap<>(schemas.size());
            for (Map.Entry<Object, McpSchemaRef> entry : schemas.entrySet()) {
                McpSchemaRef ref = entry.getValue();
                if (ref.getReference() > 1) {
                    result.put(ref.getName(), ref.getSchema());
                }
            }
            return result.isEmpty() ? null : result;
        }
    }
}
