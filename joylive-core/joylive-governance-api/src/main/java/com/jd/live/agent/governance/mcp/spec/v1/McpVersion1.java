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
package com.jd.live.agent.governance.mcp.spec.v1;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.mcp.McpToolDefinitions;
import com.jd.live.agent.governance.mcp.McpTypes;
import com.jd.live.agent.governance.mcp.McpTypes.TypeFormat;
import com.jd.live.agent.governance.mcp.McpVersion;
import com.jd.live.agent.governance.mcp.spec.JsonSchema;
import com.jd.live.agent.governance.mcp.spec.JsonSchema.JsonSchemaRef;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.jd.live.agent.governance.mcp.McpTypes.TYPE_NULL;
import static com.jd.live.agent.governance.mcp.McpTypes.TYPE_OBJECT;

@Extension(value = {"v1", "2025-06-18"}, order = 0)
public class McpVersion1 implements McpVersion {

    @Override
    public String getRevision() {
        return "2025-06-18";
    }

    @Override
    public McpToolDefinitions createDefinitions() {
        return new McpToolDefinitionsV1();
    }

    @Override
    public JsonSchema output(JsonSchema schema) {
        if (schema == null) {
            return null;
        } else if (TYPE_NULL.equals(schema.getType())) {
            return schema;
        } else if (TYPE_OBJECT.equals(schema.getType())) {
            return schema;
        }
        Map<String, JsonSchema> properties = new HashMap<>();
        properties.put(PROPERTY_RESULT, schema);
        JsonSchema result = new JsonSchema();
        result.setType(TYPE_OBJECT);
        return null;
    }

    @Override
    public Object output(Object result) {
        if (result == null) {
            return null;
        }
        TypeFormat format = McpTypes.getTypeFormat(result.getClass());
        if (TYPE_OBJECT.equals(format.getType())) {
            return result;
        } else {
            Map<String, Object> map = new HashMap<>();
            map.put(PROPERTY_RESULT, result);
            return map;
        }
    }

    private static class McpToolDefinitionsV1 implements McpToolDefinitions {

        @Override
        public <K> JsonSchemaRef create(K key, Function<K, String> nameFunc, Function<K, JsonSchema> schemaFunc) {
            return new JsonSchemaRef(nameFunc.apply(key), schemaFunc.apply(key), null);
        }

        @Override
        public Map<String, JsonSchema> getDefinitions() {
            return null;
        }
    }
}
