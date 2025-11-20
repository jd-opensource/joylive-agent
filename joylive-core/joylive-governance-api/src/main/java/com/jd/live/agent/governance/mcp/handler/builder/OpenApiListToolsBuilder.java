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
import com.jd.live.agent.governance.openapi.media.MediaType;
import com.jd.live.agent.governance.openapi.media.Schema;
import com.jd.live.agent.governance.openapi.parameters.Parameter;
import com.jd.live.agent.governance.openapi.parameters.RequestBody;
import com.jd.live.agent.governance.openapi.responses.ApiResponse;
import com.jd.live.agent.governance.openapi.responses.ApiResponses;

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
        Components components = openApi.getComponents() == null ? new Components() : openApi.getComponents();
        Map<String, PathItem> paths = openApi.getPaths();
        if (paths == null || paths.isEmpty()) {
            return result;
        }
        paths.forEach((path, item) -> {
            item = components.getPathItem(item);
            McpToolMethod method = ctx.getToolMethodByPath(path);
            Map<String, Operation> operations = item == null || method == null ? null : item.getOperations();
            if (operations == null || operations.isEmpty()) {
                return;
            }
            operations.forEach((name, op) -> result.addTool(createTool(op, version, components)));
        });
        return result;
    }

    /**
     * Creates MCP tool from OpenAPI operation.
     *
     * @param op         OpenAPI operation
     * @param version    MCP version
     * @param components OpenAPI components
     * @return MCP tool
     */
    private Tool createTool(Operation op, McpVersion version, Components components) {
        McpToolDefinitions definitions = version.createDefinitions();
        return Tool.builder()
                .name(op.getOperationId())
                .title(op.getSummary())
                .description(choose(op.getDescription(), op.getSummary()))
                .inputSchema(createInputSchema(op, definitions, components))
                .outputSchema(createOutputSchema(op, definitions, components, version))
                .defs(definitions.getDefinitions())
                .build();
    }

    /**
     * Creates input schema for a method based on OpenAPI operation parameters.
     * @param op          The OpenAPI operation
     * @param definitions The mcp schema definitions
     * @param components  OpenAPI components
     * @return JsonSchema representing the input parameters
     */
    private JsonSchema createInputSchema(Operation op, McpToolDefinitions definitions, Components components) {
        Map<String, JsonSchema> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        List<Parameter> parameters = op.getParameters();
        if (parameters != null) {
            for (Parameter p : parameters) {
                p = components.getParameter(p);
                if (p != null) {
                    if (p.required()) {
                        required.add(p.getName());
                    }
                    JsonSchema schema = createJsonSchema(p.getSchema(), p.getDescription(), definitions, components, true);
                    if (schema != null) {
                        properties.put(p.getName(), schema);
                    }
                }
            }
        }
        RequestBody body = op.getRequestBody();
        if (body != null) {
            body = components.getRequestBody(body);
            if (body != null) {
                if (body.required()) {
                    required.add(body.getName());
                }
                JsonSchema schema = createJsonSchema(body, body.getDescription(), definitions, components);
                if (schema != null) {
                    properties.put(body.getName(), schema);
                }
            }
        }
        required = required.isEmpty() ? null : required;
        return properties.isEmpty() ? null : JsonSchema.builder().type(TYPE_OBJECT).required(required).properties(properties).build();
    }

    /**
     * Converts OpenAPI Schema to JsonSchema with type handling.
     *
     * @param schema OpenAPI schema to convert
     * @param description Optional description override
     * @param definitions Schema definitions registry
     * @param components OpenAPI components for resolving references
     * @param refed Whether schema is already resolved from reference
     * @return Converted JsonSchema or null if input is null
     */
    private JsonSchema createJsonSchema(Schema schema, String description, McpToolDefinitions definitions, Components components, boolean refed) {
        Schema target = !refed ? components.getSchema(schema) : schema;
        return target == null ? null : definitions.create(target, s -> s.getName(), s -> {
            Map<String, JsonSchema> properties = null;
            JsonSchema items = null;
            TypeFormat format = McpTypes.getTypeFormat(s.getType(), s.getFormat());
            if (format.isObject()) {
                properties = createProperties(definitions, components, s.getProperties());
            } else if (format.isArray()) {
                items = createJsonSchema(s.getItems(), null, definitions, components, false);
            }
            List<String> required = s.getRequired();
            required = required == null || required.isEmpty() ? null : required;
            return JsonSchema.builder()
                    .type(format.getType())
                    .description(choose(description, s.getDescription()))
                    .properties(properties)
                    .items(items)
                    .required(required).build();
        }).getSchema();
    }

    /**
     * Converts OpenAPI schema properties to JsonSchema properties.
     *
     * @param definitions Schema definitions registry
     * @param components  OpenAPI components containing reusable schemas
     * @param properties  Map of property names to schema objects
     * @return Map of property names to JsonSchema objects, or null if input is empty
     */
    private Map<String, JsonSchema> createProperties(McpToolDefinitions definitions, Components components, Map<String, Schema> properties) {
        if (properties == null || properties.isEmpty()) {
            return null;
        }
        Map<String, JsonSchema> result = new LinkedHashMap<>();
        properties.forEach((name, schema) -> {
            JsonSchema value = createJsonSchema(schema, null, definitions, components, false);
            if (value != null) {
                result.put(name, value);
            }
        });
        return result;
    }

    /**
     * Converts RequestBody to JsonSchema by extracting schema from its media type.
     *
     * @param body        Request body to extract schema from
     * @param definitions Schema definitions registry
     * @param components  OpenAPI components containing reusable schemas
     * @return JsonSchema for the request body, or null if no suitable media type found
     */
    private JsonSchema createJsonSchema(RequestBody body, String description, McpToolDefinitions definitions, Components components) {
        MediaType mediaType = getMediaType(body.getContent());
        if (mediaType == null) {
            return null;
        }
        return createJsonSchema(mediaType.getSchema(), description, definitions, components, false);
    }

    /**
     * Creates output schema for an operation.
     *
     * @param op          The OpenAPI operation
     * @param definitions The mcp schema definitions
     * @param components  OpenAPI components
     * @param version     MCP version
     * @return Map representing the output schema
     */
    private JsonSchema createOutputSchema(Operation op, McpToolDefinitions definitions, Components components, McpVersion version) {
        ApiResponses responses = op.getResponses();
        ApiResponse response = getApiResponse(responses);
        response = responses == null ? null : components.getApiResponse(response);
        MediaType mediaType = response == null ? null : getMediaType(response.getContent());
        if (mediaType == null) {
            return null;
        }
        JsonSchema result = createJsonSchema(mediaType.getSchema(), response.getDescription(), definitions, components, false);
        return version.output(result);
    }

    /**
     * Selects appropriate MediaType from a map, prioritizing APPLICATION_JSON and ALL.
     *
     * @param mediaTypes Map of media type identifiers to MediaType objects
     * @return Selected MediaType or null if map is empty
     */
    private MediaType getMediaType(Map<String, MediaType> mediaTypes) {
        int size = mediaTypes == null ? 0 : mediaTypes.size();
        if (size == 0) {
            return null;
        } else if (size == 1) {
            return mediaTypes.values().iterator().next();
        }
        MediaType result = mediaTypes.get(MediaType.APPLICATION_JSON);
        if (result == null) {
            result = mediaTypes.get(MediaType.ALL);
        }
        return result != null ? result : mediaTypes.values().iterator().next();

    }

    /**
     * Selects appropriate ApiResponse from a map, prioritizing DEFAULT and STATUS_OK.
     *
     * @param responses Map of response identifiers to ApiResponse objects
     * @return Selected ApiResponse or null if map is empty
     */
    public ApiResponse getApiResponse(Map<String, ApiResponse> responses) {
        int size = responses == null ? 0 : responses.size();
        if (size == 0) {
            return null;
        } else if (size == 1) {
            return responses.entrySet().iterator().next().getValue();
        }
        ApiResponse result = responses.get(ApiResponses.DEFAULT);
        if (result == null) {
            result = responses.get(ApiResponses.STATUS_OK);
        }
        return result != null ? result : responses.entrySet().iterator().next().getValue();
    }

}
