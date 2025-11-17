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
        private final Map<Class<?>, JsonSchemaRef> schemas = new HashMap<>();

        private final Map<String, Class> names = new HashMap<>();

        @Override
        public JsonSchemaRef create(Class<?> cls, Function<Class<?>, JsonSchema> function) {
            JsonSchemaRef result = schemas.computeIfAbsent(cls, c -> {
                String name = getName(cls);
                return new JsonSchemaRef(name, function.apply(c), "#/$defs/" + name);
            });
            return result.hasReference() ? new JsonSchemaRef(result) : result;
        }

        @Override
        public Map<String, JsonSchema> getDefinitions() {
            Map<String, JsonSchema> result = new HashMap<>(schemas.size());
            for (Map.Entry<Class<?>, JsonSchemaRef> entry : schemas.entrySet()) {
                JsonSchemaRef ref = entry.getValue();
                if (ref.getReference() > 1) {
                    result.put(ref.getName(), ref.getSchema());
                }
            }
            return result.isEmpty() ? null : result;
        }

        private String getName(Class<?> cls) {
            String name = cls.getSimpleName();
            Class old = names.putIfAbsent(name, cls);
            if (old == null || old == cls) {
                return name;
            }
            name = cls.getName();
            names.putIfAbsent(name, cls);
            return name;
        }
    }
}
