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
package com.jd.live.agent.core.mcp.converter;

import com.jd.live.agent.core.mcp.spec.v1.JsonSchema;
import com.jd.live.agent.core.mcp.spec.v1.Tool;
import com.jd.live.agent.core.mcp.version.McpToolDefinitions;
import com.jd.live.agent.core.mcp.version.McpTypes.TypeFormat;
import com.jd.live.agent.core.mcp.version.McpVersion;
import com.jd.live.agent.core.openapi.spec.v3.*;
import com.jd.live.agent.core.openapi.spec.v3.media.MediaType;
import com.jd.live.agent.core.openapi.spec.v3.media.Schema;
import com.jd.live.agent.core.openapi.spec.v3.parameters.Parameter;
import com.jd.live.agent.core.openapi.spec.v3.parameters.RequestBody;
import com.jd.live.agent.core.openapi.spec.v3.responses.ApiResponse;
import com.jd.live.agent.core.openapi.spec.v3.responses.ApiResponses;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static com.jd.live.agent.core.mcp.version.McpTypes.TYPE_OBJECT;
import static com.jd.live.agent.core.mcp.version.McpTypes.getTypeFormat;

/**
 * Converts OpenAPI specifications to MCP Tool objects.
 */
public class OpenApiConverter {

    private final OpenApi openApi;

    private final McpVersion version;

    private final Components components;

    private final McpToolDefinitions definitions;

    public OpenApiConverter(OpenApi openApi, McpVersion version) {
        this.openApi = openApi;
        this.version = version;
        this.components = openApi.getComponents();
        this.definitions = version.createDefinitions();
    }

    /**
     * Converts an OpenAPI specification to a list of Tool objects.
     *
     * @return List of converted Tool objects
     */
    public List<Tool> convert() {
        return convert(null);
    }

    /**
     * Converts OpenAPI paths to Tool objects with operation filtering.
     *
     * @param operationFilter Function to filter or transform operations from PathItems
     * @return List of converted Tool objects
     */
    public List<Tool> convert(BiFunction<String, PathItem, List<Operation>> operationFilter) {
        List<Tool> result = new ArrayList<>();
        Components components = openApi.getComponents() == null ? new Components() : openApi.getComponents();
        Map<String, PathItem> paths = openApi.getPaths();
        if (paths == null || paths.isEmpty()) {
            return result;
        }
        paths.forEach((path, item) -> {
            ComponentRef<PathItem> ref = components.getPathItem(item);
            PathItem target = ref.getTarget();
            if (target == null) {
                //$ref is not exists.
                return;
            }
            List<Operation> operations = operationFilter == null ? target.operations() : operationFilter.apply(path, target);
            if (operations == null || operations.isEmpty()) {
                return;
            }
            operations.forEach(op -> result.add(createTool(op)));
        });
        return result;
    }

    /**
     * Creates MCP tool from OpenAPI operation.
     *
     * @param op OpenAPI operation
     * @return MCP tool
     */
    private Tool createTool(Operation op) {
        return Tool.builder()
                .name(op.getOperationId())
                .title(op.getSummary())
                .description(choose(op.getDescription(), op.getSummary()))
                .inputSchema(createInputSchema(op))
                .outputSchema(createOutputSchema(op))
                .defs(definitions.getDefinitions())
                .build();
    }

    /**
     * Creates input schema for a method based on OpenAPI operation parameters.
     *
     * @param op The OpenAPI operation
     * @return JsonSchema representing the input parameters
     */
    private JsonSchema createInputSchema(Operation op) {
        Map<String, JsonSchema> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        List<Parameter> parameters = op.getParameters();
        if (parameters != null) {
            for (Parameter p : parameters) {
                ComponentRef<Parameter> ref = components.getParameter(p);
                Parameter target = ref.getTarget();
                if (target == null) {
                    //$ref is not exists.
                    continue;
                }
                if (p.required()) {
                    required.add(p.getName());
                }
                JsonSchema schema = createJsonSchema(target.getSchema(), choose(p.getDescription(), target.getDescription()));
                if (schema != null) {
                    schema.setIn(choose(p.getIn(), target.getIn()));
                    properties.put(p.getName(), schema);
                }
            }
        }
        RequestBody body = op.getRequestBody();
        if (body != null) {
            ComponentRef<RequestBody> ref = components.getRequestBody(body);
            RequestBody target = ref.getTarget();
            if (target != null) {
                if (body.required()) {
                    required.add(body.getName());
                }
                JsonSchema schema = createJsonSchema(body, body.getDescription());
                if (schema != null) {
                    schema.setIn("body");
                    properties.put(body.getName(), schema);
                }
            }
        }
        required = required.isEmpty() ? null : required;
        // inputSchema is not null in spec.
        return JsonSchema.builder().type(TYPE_OBJECT).required(required).properties(properties).build();
    }

    /**
     * Converts OpenAPI Schema to JsonSchema with type handling.
     *
     * @param schema      OpenAPI schema to convert
     * @param description Optional description override
     * @return Converted JsonSchema or null if input is null
     */
    private JsonSchema createJsonSchema(Schema schema, String description) {
        if (schema == null) {
            return null;
        }
        ComponentRef<Schema> ref = components.getSchema(schema);
        Schema target = ref.getTarget();
        return target == null ? null : definitions.create(target, s -> ref.getComponent(), s -> {
            Map<String, JsonSchema> properties = null;
            JsonSchema items = null;
            TypeFormat format = getTypeFormat(s.getType(), s.getFormat());
            if (format.isObject()) {
                properties = createProperties(s.getProperties());
            } else if (format.isArray()) {
                items = createJsonSchema(s.getItems(), null);
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
     * @param properties  Map of property names to schema objects
     * @return Map of property names to JsonSchema objects, or null if input is empty
     */
    private Map<String, JsonSchema> createProperties(Map<String, Schema> properties) {
        if (properties == null || properties.isEmpty()) {
            return null;
        }
        Map<String, JsonSchema> result = new LinkedHashMap<>();
        properties.forEach((name, schema) -> {
            JsonSchema value = createJsonSchema(schema, null);
            if (value != null) {
                result.put(name, value);
            }
        });
        return result;
    }

    /**
     * Converts RequestBody to JsonSchema by extracting schema from its media type.
     *
     * @param body Request body to extract schema from
     * @return JsonSchema for the request body, or null if no suitable media type found
     */
    private JsonSchema createJsonSchema(RequestBody body, String description) {
        MediaType mediaType = getMediaType(body.getContent());
        if (mediaType == null) {
            return null;
        }
        return createJsonSchema(mediaType.getSchema(), description);
    }

    /**
     * Creates output schema for an operation.
     *
     * @param op The OpenAPI operation
     * @return Map representing the output schema
     */
    private JsonSchema createOutputSchema(Operation op) {
        ApiResponses responses = op.getResponses();
        ApiResponse response = getApiResponse(responses);
        ComponentRef<ApiResponse> ref = response == null ? null : components.getApiResponse(response);
        ApiResponse target = ref == null ? null : ref.getTarget();
        MediaType mediaType = target == null ? null : getMediaType(target.getContent());
        if (mediaType == null) {
            return null;
        }
        JsonSchema result = createJsonSchema(mediaType.getSchema(), choose(response.getDescription(), target.getDescription()));
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
    private ApiResponse getApiResponse(Map<String, ApiResponse> responses) {
        int size = responses == null ? 0 : responses.size();
        if (size == 0) {
            return null;
        } else if (size == 1) {
            return responses.entrySet().iterator().next().getValue();
        }
        ApiResponse result = responses.get(ApiResponses.STATUS_OK);
        if (result == null) {
            result = responses.get(ApiResponses.DEFAULT);
        }
        return result != null ? result : responses.entrySet().iterator().next().getValue();
    }

    private String choose(String value1, String value2) {
        return value1 != null && !value1.isEmpty() ? value1 : value2;
    }

}
