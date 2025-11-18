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
package com.jd.live.agent.governance.mcp.handler.builder;

import com.jd.live.agent.governance.mcp.McpRequestContext;
import com.jd.live.agent.governance.mcp.McpToolMethod;
import com.jd.live.agent.governance.mcp.McpTypes;
import com.jd.live.agent.governance.mcp.McpTypes.TypeFormat;
import com.jd.live.agent.governance.mcp.spec.JsonSchema;
import com.jd.live.agent.governance.mcp.spec.ListToolsResult;
import com.jd.live.agent.governance.mcp.spec.Tool;
import com.jd.live.agent.governance.openapi.OpenApi;
import com.jd.live.agent.governance.openapi.Operation;
import com.jd.live.agent.governance.openapi.PathItem;
import com.jd.live.agent.governance.openapi.media.Schema;
import com.jd.live.agent.governance.openapi.parameters.Parameter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.StringUtils.choose;
import static com.jd.live.agent.governance.mcp.McpTypes.TYPE_OBJECT;

/**
 * OpenAPI-based implementation of ListToolsBuilder that creates tools from OpenAPI specifications.
 * Results are cached after first creation.
 */
public class OpenApiListToolsBuilder implements ListToolsBuilder {

    public static final ListToolsBuilder INSTANCE = new OpenApiListToolsBuilder();

    private ListToolsResult cache;

    @Override
    public ListToolsResult create(McpRequestContext ctx) {
        if (cache == null) {
            ListToolsResult result = new ListToolsResult();
            OpenApi openApi = ctx.getOpenApi().get();
            Map<String, PathItem> paths = openApi.getPaths();
            if (paths == null || paths.isEmpty()) {
                return result;
            }
            paths.forEach((path, item) -> {
                Map<String, Operation> operations = item.getOperations();
                if (operations == null || operations.isEmpty()) {
                    return;
                }
                McpToolMethod method = ctx.getToolMethodByPath(path);
                if (method == null) {
                    return;
                }
                operations.forEach((name, op) -> {
                    result.addTool(createTool(op, method));
                });
            });
            cache = result;
        }
        return cache;
    }

    /**
     * Creates a Tool from an OpenAPI operation and method definition.
     *
     * @param op     The OpenAPI operation
     * @param method The corresponding MCP tool method
     * @return A Tool representation
     */
    private Tool createTool(Operation op, McpToolMethod method) {
        return Tool.builder()
                .name(method.getName())
                .title(op.getSummary())
                .description(choose(op.getDescription(), op.getSummary()))
                .inputSchema(createInputSchema(op, method))
                .outputSchema(createOutputSchema(op))
                .build();
    }

    /**
     * Creates input schema for a method based on OpenAPI operation parameters.
     *
     * @param op     The OpenAPI operation
     * @param method The MCP tool method
     * @return JsonSchema representing the input parameters
     */
    private JsonSchema createInputSchema(Operation op, McpToolMethod method) {
        Map<String, JsonSchema> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        List<Parameter> parameters = op.getParameters();
        if (parameters != null) {
            for (Parameter p : parameters) {
                if (p.getRequired() != null && p.getRequired()) {
                    required.add(p.getName());
                }
                Schema schema = p.getSchema();
                properties.put(p.getName(), createJsonSchema(schema));
            }
        }
        required = required.isEmpty() ? null : required;

        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        return JsonSchema.builder().type(TYPE_OBJECT).required(required).properties(properties).build();
    }

    /**
     * Creates a JsonSchema from an OpenAPI Schema object.
     *
     * @param schema The OpenAPI Schema to convert
     * @return JsonSchema representation
     */
    private static JsonSchema createJsonSchema(Schema schema) {
        Map<String, Object> properties = null;
        TypeFormat format = McpTypes.getTypeFormat(schema.getType(), schema.getFormat());
        // TODO array
        if (TYPE_OBJECT.equals(format.getType())) {
            properties = new LinkedHashMap<>();
            if (schema.getProperties() != null) {
                for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
                    properties.put(entry.getKey(), createJsonSchema(entry.getValue()));
                }
            }
        }
        return JsonSchema.builder().type(format.getType()).build();
    }

    /**
     * Creates output schema for an operation.
     *
     * @param op The OpenAPI operation
     * @return Map representing the output schema
     */
    private JsonSchema createOutputSchema(Operation op) {
        return null;
    }

}
