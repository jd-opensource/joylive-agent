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

import com.jd.live.agent.governance.mcp.*;
import com.jd.live.agent.governance.mcp.McpTypes.TypeFormat;
import com.jd.live.agent.governance.mcp.spec.JsonSchema;
import com.jd.live.agent.governance.mcp.spec.ListToolsResult;
import com.jd.live.agent.governance.mcp.spec.Tool;
import com.jd.live.agent.governance.openapi.Components;
import com.jd.live.agent.governance.openapi.OpenApi;
import com.jd.live.agent.governance.openapi.Operation;
import com.jd.live.agent.governance.openapi.PathItem;
import com.jd.live.agent.governance.openapi.media.Schema;
import com.jd.live.agent.governance.openapi.parameters.Parameter;
import com.jd.live.agent.governance.openapi.parameters.RequestBody;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.StringUtils.choose;
import static com.jd.live.agent.core.util.StringUtils.isEmpty;
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
            cache = doCreate(ctx);
        }
        return cache;
    }

    /**
     * Creates a result containing available tools based on the given context.
     *
     * @param ctx The MCP request context
     * @return The list tools result
     */
    private ListToolsResult doCreate(McpRequestContext ctx) {
        ListToolsResult result = new ListToolsResult();
        OpenApi openApi = ctx.getOpenApi().get();
        McpVersion version = ctx.getVersion();
        Components components = openApi.getComponents();
        Map<String, PathItem> paths = openApi.getPaths();
        if (paths == null || paths.isEmpty()) {
            return result;
        }
        paths.forEach((path, item) -> {
            if (!isEmpty(item.getRef())) {
                item = components == null ? null : components.getPathItem(item.getRef());
            }
            McpToolMethod method = ctx.getToolMethodByPath(path);
            Map<String, Operation> operations = item == null || method == null ? null : item.getOperations();
            if (operations == null || operations.isEmpty()) {
                return;
            }
            operations.forEach((name, op) -> result.addTool(createTool(version, components, op, method)));
        });
        return result;
    }

    /**
     * Creates MCP tool from OpenAPI operation.
     *
     * @param version    MCP version
     * @param components OpenAPI components
     * @param op         OpenAPI operation
     * @param method     MCP tool method
     * @return MCP tool
     */
    private Tool createTool(McpVersion version, Components components, Operation op, McpToolMethod method) {
        McpToolDefinitions definitions = version.createDefinitions();
        return Tool.builder()
                .name(op.getOperationId())
                .title(op.getSummary())
                .description(choose(op.getDescription(), op.getSummary()))
                .inputSchema(createInputSchema(version, components, op, method))
                .outputSchema(createOutputSchema(version, components, op))
                .defs(definitions.getDefinitions())
                .build();
    }

    /**
     * Creates input schema for a method based on OpenAPI operation parameters.
     * @param version    MCP version
     * @param components OpenAPI components
     * @param op         The OpenAPI operation
     * @param method     The MCP tool method
     * @return JsonSchema representing the input parameters
     */
    private JsonSchema createInputSchema(McpVersion version, Components components, Operation op, McpToolMethod method) {
        Map<String, JsonSchema> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        List<Parameter> parameters = op.getParameters();
        if (parameters != null) {
            for (Parameter p : parameters) {
                if (!isEmpty(p.getRef())) {
                    p = components == null ? null : components.getParameter(p.getRef());
                }
                if (p != null) {
                    if (p.getRequired() != null && p.getRequired()) {
                        required.add(p.getName());
                    }
                    Schema schema = p.getSchema();
                    properties.put(p.getName(), createJsonSchema(schema));
                }
            }
        }
        RequestBody body = op.getRequestBody();
        if (body != null) {
            if (!isEmpty(body.getRef())) {
                body = components == null ? null : components.getRequestBody(body.getRef());
            }
            if (body != null) {
                String key = isEmpty(body.getName()) ? "body" : body.getName();

                properties.put(key, createJsonSchema(body));
            }
        }
        required = required.isEmpty() ? null : required;
        // TODO request body
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
    private JsonSchema createJsonSchema(Schema schema) {
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

    private JsonSchema createJsonSchema(RequestBody body) {
        return null;
    }

    /**
     * Creates output schema for an operation.
     *
     * @param op The OpenAPI operation
     * @return Map representing the output schema
     */
    private JsonSchema createOutputSchema(McpVersion version, Components components, Operation op) {
        return null;
    }

}
