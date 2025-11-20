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

import com.jd.live.agent.core.parser.JsonSchemaParser;
import com.jd.live.agent.core.parser.JsonSchemaParser.FieldSchema;
import com.jd.live.agent.governance.mcp.*;
import com.jd.live.agent.governance.mcp.spec.JsonSchema;
import com.jd.live.agent.governance.mcp.spec.JsonSchema.JsonSchemaRef;
import com.jd.live.agent.governance.mcp.spec.ListToolsResult;
import com.jd.live.agent.governance.mcp.spec.Tool;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static com.jd.live.agent.governance.mcp.McpTypes.*;

/**
 * A ListToolsBuilder implementation that uses reflection to build tools list.
 * Results are cached after first creation.
 */
public class ReflectionListToolsBuilder implements ListToolsBuilder {

    public static final ListToolsBuilder INSTANCE = new ReflectionListToolsBuilder();

    private final Map<String, Class> names = new HashMap<>();

    private ListToolsResult cache;

    @Override
    public ListToolsResult create(McpRequestContext ctx) {
        if (cache == null) {
            ListToolsResult result = new ListToolsResult();
            Map<String, McpToolMethod> methods = ctx.getMethods();
            if (methods != null && !methods.isEmpty()) {
                methods.forEach((k, m) -> result.addTool(createTool(m, ctx)));
            }
            cache = result;
        }
        return cache;
    }

    /**
     * Creates a Tool from a method definition.
     *
     * @param method The method to convert to a tool
     * @param ctx    The request context containing schema parsing configuration and preferences
     * @return A Tool representation of the method
     */
    private Tool createTool(McpToolMethod method, McpRequestContext ctx) {
        McpVersion version = ctx.getVersion();
        McpToolDefinitions definitions = version.createDefinitions();
        return Tool.builder()
                .name(method.getName())
                .title(method.getName())
                .description(method.getName())
                .inputSchema(createInputSchema(method, definitions, ctx.getJsonSchemaParser()))
                .outputSchema(version.output(createOutputSchema(method, definitions, ctx.getJsonSchemaParser())))
                .defs(definitions.getDefinitions())
                .build();
    }

    /**
     * Creates an input schema for a method based on its parameters.
     *
     * @param method      The method to create input schema for
     * @param definitions Collection of definitions used for reference resolution
     * @param parser      JSON schema parser used to analyze parameter types
     * @return JsonSchema representing the input parameters structure, or null if the method has no parameters
     */
    private JsonSchema createInputSchema(McpToolMethod method, McpToolDefinitions definitions, JsonSchemaParser parser) {
        McpToolParameter[] parameters = method.getParameters();
        if (parameters == null || parameters.length == 0) {
            return null;
        }
        Map<String, JsonSchema> properties = new LinkedHashMap<>(parameters.length);
        List<String> required = new ArrayList<>();
        for (McpToolParameter parameter : parameters) {
            // filter system parameters
            if (parameter.getSystemParser() == null) {
                if (parameter.isRequired()) {
                    required.add(parameter.getName());
                }
                JsonSchema schema = createJsonSchema(parameter.getType(), parameter.getGenericType(), definitions, parser);
                properties.put(parameter.getName(), schema);
            }
        }
        required = required.isEmpty() ? null : required;
        return JsonSchema.builder().type(TYPE_OBJECT).properties(properties).required(required).build();
    }

    /**
     * Creates output schema for a method.
     *
     * @param method      The method to create output schema for
     * @param definitions Collection of definitions used for reference resolution
     * @param parser      JSON schema parser used to analyze parameter and return types
     * @return Map representing the output schema
     */
    private JsonSchema createOutputSchema(McpToolMethod method, McpToolDefinitions definitions, JsonSchemaParser parser) {
        return createJsonSchema(method.getReturnType(), method.getGenericReturnType(), definitions, parser);
    }

    /**
     * Creates a JsonSchema for a given Java type.
     *
     * @param cls         The Java class to create schema for
     * @param definitions Collection of definitions used for reference resolution
     * @param parser      JSON schema parser used to analyze parameter and return types
     * @return JsonSchema representing the type
     */
    private JsonSchema createJsonSchema(Class<?> cls, Type type, McpToolDefinitions definitions, JsonSchemaParser parser) {
        TypeFormat format = McpTypes.getTypeFormat(cls);
        String schemaType = format.getType();
        if (TYPE_NULL.equals(format.getType())) {
            return null;
        } else if (TYPE_OBJECT.equals(format.getType())) {
            return createSchema(definitions, cls, schemaType, createProperties(cls, definitions, parser), null);
        } else if (TYPE_ARRAY.equals(format.getType())) {
            JsonSchema itemSchema = null;
            if (cls.isArray()) {
                if (type instanceof Class<?>) {
                    itemSchema = createJsonSchema(cls.getComponentType(), cls.getComponentType(), definitions, parser);
                } else if (type instanceof GenericArrayType) {
                    Type componentType = ((GenericArrayType) type).getGenericComponentType();
                    if (componentType instanceof Class<?>) {
                        itemSchema = createJsonSchema((Class<?>) componentType, componentType, definitions, parser);
                    }
                }
            } else {
                if (type instanceof ParameterizedType) {
                    Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
                    if (typeArgs != null && typeArgs.length == 1 && typeArgs[0] instanceof Class<?>) {
                        itemSchema = createJsonSchema((Class<?>) typeArgs[0], typeArgs[0], definitions, parser);
                    }
                }
            }
            return JsonSchema.builder().type(schemaType).properties(null).items(itemSchema).build();
        }
        return JsonSchema.builder().type(schemaType).properties(null).items(null).build();
    }

    /**
     * Creates a properties map for a given class.
     *
     * @param cls         The class to create properties for
     * @param definitions Collection of definitions used for reference resolution
     * @param parser      JSON schema parser used to analyze parameter and return types
     * @return A map of property names to their schema definitions, or null if the class is a Map
     */
    private Map<String, JsonSchema> createProperties(Class<?> cls, McpToolDefinitions definitions, JsonSchemaParser parser) {
        if (Map.class.isAssignableFrom(cls)) {
            return null;
        }
        Map<String, JsonSchema> properties;
        properties = new LinkedHashMap<>();
        List<FieldSchema> fieldSchemas = parser.describe(cls);
        if (fieldSchemas != null) {
            for (FieldSchema schema : fieldSchemas) {
                Field field = schema.getField();
                properties.put(schema.getName(), createJsonSchema(field.getType(), field.getGenericType(), definitions, parser));
            }
        }
        return properties;
    }

    /**
     * Creates a JSON schema for the specified class.
     * <p>
     * If the schema already exists in definitions, returns a reference to it.
     * Otherwise, creates a new schema with the provided properties.
     *
     * @param definitions the container for schema definitions
     * @param cls         the class to create schema for
     * @param type        the JSON schema type
     * @param properties  the schema properties (for object types)
     * @param items       the schema for array items (for array types)
     * @return the created or referenced JSON schema
     */
    private JsonSchema createSchema(McpToolDefinitions definitions,
                                    Class<?> cls,
                                    String type,
                                    Map<String, JsonSchema> properties,
                                    JsonSchema items) {
        JsonSchemaRef ref = definitions.create(cls, this::getName,
                c -> JsonSchema.builder().type(type).properties(properties).items(items).build());
        if (ref.addReference() > 1) {
            ref.ref();
        }
        return ref.getSchema();
    }

    /**
     * Gets a unique name for the class, using simple name when possible.
     * Falls back to fully qualified name to resolve conflicts.
     *
     * @param cls Class to get name for
     * @return Unique name for the class
     */
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
