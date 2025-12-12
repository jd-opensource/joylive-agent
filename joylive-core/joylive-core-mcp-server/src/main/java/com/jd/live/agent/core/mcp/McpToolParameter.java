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
package com.jd.live.agent.core.mcp;

import com.jd.live.agent.core.mcp.exception.JsonRpcException.MissingParameterException;
import com.jd.live.agent.core.util.converter.Converter;
import com.jd.live.agent.core.util.type.ClassUtils;
import lombok.Getter;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.type.ClassUtils.isSimpleValueType;

/**
 * Represents a parameter definition for Model Context Protocol (MCP) tool.
 * <p>
 * This class encapsulates metadata and behavior for tool parameter handling, including
 * parameter extraction from various sources, type conversion, validation, and default value
 * processing. Parameters can be configured with different locations (query, path, header,
 * cookie, body, system) and processing options.
 * <p>
 * Use the {@link McpToolParameterBuilder} to construct instances with the desired configuration.
 *
 * @see McpToolParameterBuilder
 * @see Location
 */
@Getter
public class McpToolParameter {

    /**
     * The Java reflection parameter this tool parameter represents.
     */
    private final Parameter parameter;

    /**
     * The name of the parameter.
     */
    private final String name;

    /**
     * The index position of the parameter in the method signature.
     */
    private final int index;

    /**
     * The Java class type of the parameter.
     */
    private final Class<?> type;

    /**
     * The generic type of the parameter, including type parameters.
     */
    private final Type genericType;

    /**
     * The argument name used in requests, if different from parameter name.
     */
    private final String arg;

    /**
     * The actual type to convert values to, which may differ from declared type.
     */
    private final Type actualType;

    /**
     * Whether this parameter is required for the tool execution.
     */
    private final boolean required;

    /**
     * Whether this parameter is optional for the tool execution.
     */
    private final boolean optional;

    /**
     * The source location where the parameter value should be extracted from.
     */
    private final Location location;

    /**
     * Whether the parameter value should be converted to the target type.
     */
    private final boolean convertable;

    /**
     * Custom converter for transforming parameter values.
     */
    private final Converter<Object, Object> converter;

    /**
     * Wrapper converter applied after main conversion.
     */
    private final Converter<Object, Object> wrapper;

    /**
     * Parser for system-generated parameter values.
     */
    private final McpRequestParser parser;

    /**
     * Parser for default values when parameter is not provided.
     */
    private final McpRequestParser defaultParser;

    private final Validator validator;

    public McpToolParameter(Parameter parameter,
                            int index,
                            Type actualType,
                            String arg,
                            boolean required,
                            boolean optional,
                            Location location,
                            boolean convertable,
                            Converter<Object, Object> converter,
                            Converter<Object, Object> wrapper,
                            McpRequestParser parser,
                            McpRequestParser defaultParser,
                            Validator validator) {
        this.parameter = parameter;
        this.name = parameter.getName();
        this.arg = arg;
        this.index = index;
        this.type = parameter.getType();
        this.genericType = parameter.getParameterizedType();
        this.actualType = actualType;
        this.required = required;
        this.optional = optional;
        this.location = location;
        this.convertable = convertable;
        this.converter = converter;
        this.wrapper = wrapper;
        this.parser = parser;
        this.defaultParser = defaultParser;
        this.validator = validator;
    }

    /**
     * Gets the key used to look up this parameter's value in the request.
     * Returns the argument name if specified, otherwise falls back to the parameter name.
     *
     * @return The key for parameter lookup
     */
    public String getKey() {
        if (arg == null || arg.isEmpty()) {
            return name;
        }
        return arg;
    }

    /**
     * Gets the actual class type for this parameter, resolving generic types when possible.
     *
     * @return The resolved class type
     */
    public Class<?> getActualClass() {
        return actualType instanceof Class ? (Class<?>) actualType : type;
    }

    /**
     * Parses and processes the parameter value from the request.
     * This method handles extraction, default values, type conversion, and validation.
     *
     * @param request The MCP request containing parameter values
     * @param ctx     The request context for conversion and processing
     * @return The processed parameter value
     * @throws Exception                 If parameter extraction or processing fails
     * @throws MissingParameterException If a required parameter is missing
     */
    public Object parse(McpRequest request, McpRequestContext ctx) throws Exception {
        Object result = getValue(request, ctx);
        boolean empty = result == null || result instanceof CharSequence && ((CharSequence) result).length() == 0;
        result = empty && defaultParser != null ? defaultParser.parse(request, ctx) : result;
        result = result == null || !convertable ? result : convert(ctx, result);
        if (result == null && required) {
            throw new MissingParameterException(name);
        }
        return wrapper == null ? result : wrapper.convert(result);
    }

    /**
     * Extracts the parameter value from the appropriate location in the request.
     * The location is determined by the parameter's configured location type.
     *
     * @param request The MCP request containing parameter values
     * @param ctx     The request context for processing
     * @return The extracted parameter value
     * @throws Exception If value extraction fails
     */
    private Object getValue(McpRequest request, McpRequestContext ctx) throws Exception {
        String key = getKey();
        if (parser != null) {
            return parser.parse(request, ctx);
        }
        switch (location) {
            case HEADER:
                return unary(request.getHeader(key), false);
            case COOKIE:
                return unary(request.getCookie(key), true);
            case PATH:
                return request.getPath(key);
            case BODY:
                return request.getBody(key);
            case QUERY:
            default:
                return unary(request.getQuery(key), false);
        }
    }

    /**
     * Handles list values by extracting a single item when appropriate.
     * For parameters that don't expect collections, returns the first item.
     *
     * @param value The value to process, potentially a list
     * @param first Whether to always take the first item regardless of parameter type
     * @return The processed value, either the original or a single item from a list
     */
    private Object unary(Object value, boolean first) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return null;
            } else if (list.size() == 1 || first) {
                return list.get(0);
            }
            Class<?> actualClass = getActualClass();
            if (!actualClass.isArray() && !Collection.class.isAssignableFrom(actualClass)) {
                return list.get(0);
            }
        }
        return value;
    }

    /**
     * Converts the parameter value to the expected type.
     * Uses custom converters if configured, otherwise falls back to the context converter.
     *
     * @param ctx    The request context containing the default converter
     * @param target The value to convert
     * @return The converted value
     */
    private Object convert(McpRequestContext ctx, Object target) {
        if (converter != null) {
            // custom converter
            return converter.convert(target);
        }
        // default converter
        Class<?> resultClass = getActualClass();
        Class<?> targetClass = target.getClass();
        if (resultClass.isInstance(target)) {
            if (isSimpleType(targetClass)) {
                return target;
            }
        }
        return ctx.getConverter().convert(target, actualType);
    }

    /**
     * Checks if this parameter is a map type.
     *
     * @return True if the parameter type is assignable to Map
     */
    private boolean isMapType() {
        return Map.class.isAssignableFrom(type);
    }

    /**
     * Checks if the given type is a simple value type.
     *
     * @param type The type to check
     * @return True if the type is a simple value type (primitive, wrapper, etc.)
     */
    private boolean isSimpleType(Class<?> type) {
        return isSimpleValueType(type);
    }

    public void validate(Object arg) throws Exception {
        if (validator != null) {
            validator.validate(arg);
        }
    }

    /**
     * Creates a new builder for constructing McpToolParameter instances.
     *
     * @return A new McpToolParameterBuilder instance
     */
    public static McpToolParameterBuilder builder() {
        return new McpToolParameterBuilder();
    }

    /**
     * Builder class for creating McpToolParameter instances with a fluent API.
     * Provides methods for configuring all aspects of a parameter definition.
     */
    public static class McpToolParameterBuilder {
        private Parameter parameter;
        private String name;
        private int index;
        private Class<?> type;
        private Type genericType;
        private String arg;
        private Type actualType;
        private boolean required;
        private boolean optional;
        private Location location;
        private boolean convertable = true;
        private Converter<Object, Object> converter;
        private Converter<Object, Object> wrapper;
        private McpRequestParser parser;
        private McpRequestParser defaultParser;
        private Validator validator;

        /**
         * Sets the Java reflection parameter and initializes related fields.
         *
         * @param parameter The Java reflection parameter
         * @return This builder instance for method chaining
         */
        public McpToolParameterBuilder parameter(Parameter parameter) {
            this.parameter = parameter;
            this.name = parameter == null ? null : parameter.getName();
            this.type = parameter == null ? null : parameter.getType();
            this.genericType = parameter == null ? null : parameter.getParameterizedType();
            this.actualType = genericType;
            return this;
        }

        /**
         * Sets the parameter name.
         *
         * @param name The parameter name
         * @return This builder instance for method chaining
         */
        public McpToolParameterBuilder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the parameter index in the method signature.
         *
         * @param index The parameter index
         * @return This builder instance for method chaining
         */
        public McpToolParameterBuilder index(int index) {
            this.index = index;
            return this;
        }

        /**
         * Sets the actual type for conversion, which may differ from the declared type.
         *
         * @param actualType The actual type for conversion
         * @return This builder instance for method chaining
         */
        public McpToolParameterBuilder actualType(Type actualType) {
            this.actualType = actualType;
            return this;
        }

        /**
         * Sets the argument name used in requests, if different from parameter name.
         *
         * @param arg The argument name
         * @return This builder instance for method chaining
         */
        public McpToolParameterBuilder arg(String arg) {
            this.arg = arg;
            return this;
        }

        /**
         * Sets whether the parameter is required.
         *
         * @param required True if the parameter is required
         * @return This builder instance for method chaining
         */
        public McpToolParameterBuilder required(boolean required) {
            this.required = required;
            return this;
        }

        /**
         * Sets whether the parameter is optional.
         *
         * @param optional True if the parameter is optional
         * @return This builder instance for method chaining
         */
        public McpToolParameterBuilder optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        /**
         * Sets whether the parameter value should be converted to the target type.
         *
         * @param convertable True if conversion should be applied
         * @return This builder instance for method chaining
         */
        public McpToolParameterBuilder convertable(boolean convertable) {
            this.convertable = convertable;
            return this;
        }

        /**
         * Sets a custom converter for transforming parameter values.
         * If a converter is already set, the new converter will be chained after it.
         *
         * @param converter The converter to use
         * @return This builder instance for method chaining
         */
        public McpToolParameterBuilder converter(Converter<Object, Object> converter) {
            if (converter != null) {
                this.converter = this.converter == null ? converter : this.converter.then(converter);
            }
            return this;
        }

        /**
         * Sets a wrapper converter to be applied after the main conversion.
         * If a wrapper is already set, the new wrapper will be chained after it.
         *
         * @param wrapper The wrapper converter to use
         * @return This builder instance for method chaining
         */
        public McpToolParameterBuilder wrapper(Converter<Object, Object> wrapper) {
            if (wrapper != null) {
                this.wrapper = this.wrapper == null ? wrapper : this.wrapper.then(wrapper);
            }
            return this;
        }

        /**
         * Sets the source location where the parameter value should be extracted from.
         *
         * @param location The parameter source location
         * @return This builder instance for method chaining
         */
        public McpToolParameterBuilder location(Location location) {
            this.location = location;
            return this;
        }

        /**
         * Sets the parser for system-generated parameter values.
         *
         * @param parser The system parser to use
         * @return This builder instance for method chaining
         */
        public McpToolParameterBuilder parser(McpRequestParser parser) {
            this.parser = parser;
            return this;
        }

        /**
         * Sets the parser for default values when parameter is not provided.
         *
         * @param defaultValueParser The default value parser to use
         * @return This builder instance for method chaining
         */
        public McpToolParameterBuilder defaultValueParser(McpRequestParser defaultValueParser) {
            this.defaultParser = defaultValueParser;
            return this;
        }

        public McpToolParameterBuilder validator(Validator validator) {
            this.validator = validator;
            return this;
        }

        /**
         * Gets the Java reflection parameter.
         *
         * @return The parameter
         */
        public Parameter parameter() {
            return this.parameter;
        }

        /**
         * Gets the parameter name.
         *
         * @return The parameter name
         */
        public String name() {
            return name;
        }

        /**
         * Gets the parameter index in the method signature.
         *
         * @return The parameter index
         */
        public int index() {
            return this.index;
        }

        /**
         * Gets the Java class type of the parameter.
         *
         * @return The parameter type
         */
        public Class<?> type() {
            return this.type;
        }

        /**
         * Gets the generic type of the parameter.
         *
         * @return The generic type
         */
        public Type genericType() {
            return this.genericType;
        }

        /**
         * Gets the actual type for conversion.
         *
         * @return The actual type
         */
        public Type actualType() {
            return this.actualType;
        }

        /**
         * Gets the argument name used in requests.
         *
         * @return The argument name
         */
        public String arg() {
            return this.arg;
        }

        /**
         * Gets the key used to look up this parameter's value in the request.
         * Returns the argument name if specified, otherwise falls back to the parameter name.
         *
         * @return The key for parameter lookup
         */
        public String key() {
            if (arg == null || arg.isEmpty()) {
                return name;
            }
            return arg;
        }

        /**
         * Gets whether the parameter is required.
         *
         * @return True if the parameter is required
         */
        public boolean required() {
            return this.required;
        }

        /**
         * Gets whether the parameter is optional.
         *
         * @return True if the parameter is optional
         */
        public boolean optional() {
            return this.optional;
        }

        /**
         * Gets the source location where the parameter value should be extracted from.
         *
         * @return The parameter source location
         */
        public Location location() {
            return this.location;
        }

        /**
         * Gets whether the parameter value should be converted to the target type.
         *
         * @return True if conversion should be applied
         */
        public boolean convertable() {
            return convertable;
        }

        /**
         * Gets the custom converter for transforming parameter values.
         *
         * @return The converter
         */
        public Converter<Object, Object> converter() {
            return this.converter;
        }

        /**
         * Gets the wrapper converter applied after the main conversion.
         *
         * @return The wrapper converter
         */
        public Converter<Object, Object> wrapper() {
            return this.wrapper;
        }

        /**
         * Gets the parser for system-generated parameter values.
         *
         * @return The system parser
         */
        public McpRequestParser parser() {
            return this.parser;
        }

        /**
         * Gets the parser for default values when parameter is not provided.
         *
         * @return The default value parser
         */
        public McpRequestParser defaultValueParser() {
            return this.defaultParser;
        }

        public Validator validator() {
            return this.validator;
        }

        /**
         * Gets the actual class type for this parameter, resolving generic types when possible.
         *
         * @return The resolved class type
         */
        public Class<?> actualClass() {
            return actualType instanceof Class ? (Class<?>) actualType : type;
        }

        /**
         * Checks if the parameter is of the specified type.
         *
         * @param targetClass The class to check against
         * @return True if the parameter is of the specified type
         */
        public boolean isType(Class<?> targetClass) {
            return type == targetClass || actualType == targetClass;
        }

        /**
         * Checks if the parameter is assignable to the specified type.
         *
         * @param targetClass The class to check against
         * @return True if the parameter is assignable to the specified type
         */
        public boolean isAssignableTo(Class<?> targetClass) {
            return targetClass.isAssignableFrom(type) || actualType instanceof Class<?> && ((Class<?>) actualType).isAssignableFrom(targetClass);
        }

        /**
         * Checks if the parameter is a simple value type.
         *
         * @return True if the parameter is a simple value type (primitive, wrapper, etc.)
         */
        public boolean isSimpleType() {
            return isSimpleValueType(actualClass());
        }

        /**
         * Checks if the parameter is an entity type.
         *
         * @return True if the parameter is an entity type
         */
        public boolean isEntityType() {
            return ClassUtils.isEntity(actualClass());
        }

        /**
         * Builds a new McpToolParameter instance with the configured settings.
         *
         * @return A new McpToolParameter instance
         */
        public McpToolParameter build() {
            return new McpToolParameter(parameter, index, actualType, arg, required, optional, location,
                    convertable, converter, wrapper, parser, defaultParser, validator);
        }
    }

    /**
     * Parameter source locations.
     */
    public enum Location {
        /**
         * Query string parameters.
         */
        QUERY("query"),
        /**
         * URL path variables.
         */
        PATH("path"),
        /**
         * HTTP headers.
         */
        HEADER("header"),
        /**
         * HTTP cookies.
         */
        COOKIE("cookie"),
        /**
         * Request body.
         */
        BODY("body"),
        /**
         * System-generated parameters.
         */
        SYSTEM("system");

        private String value;

        Location(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Location fromValue(String value) {
            if (value == null) {
                return null;
            }
            for (Location location : Location.values()) {
                if (value.equals(location.value)) {
                    return location;
                }
            }
            return null;
        }
    }

}
