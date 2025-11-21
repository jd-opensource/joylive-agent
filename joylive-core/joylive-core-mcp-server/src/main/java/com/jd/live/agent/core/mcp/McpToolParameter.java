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

import com.jd.live.agent.core.mcp.spec.v1.JsonRpcException.MissingParameterException;
import com.jd.live.agent.core.util.converter.Converter;
import com.jd.live.agent.core.util.type.ClassUtils;
import lombok.Getter;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Represents a parameter definition for MCP tool.
 */
@Getter
public class McpToolParameter {

    private final Parameter parameter;

    private final String name;

    private final int index;

    private final Class<?> type;

    private final Type genericType;

    private final String arg;

    private final Type actualType;

    private final boolean required;

    private final boolean optional;

    private final Location location;

    private final boolean convertable;

    private final Converter<Object, Object> converter;

    private final Converter<Object, Object> wrapper;

    private final McpRequestParser systemParser;

    private final McpRequestParser defaultValueParser;

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
                            McpRequestParser systemParser,
                            McpRequestParser defaultValueParser) {
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
        this.systemParser = systemParser;
        this.defaultValueParser = defaultValueParser;
    }

    public String getKey() {
        if (arg == null || arg.isEmpty()) {
            return name;
        }
        return arg;
    }

    public Class<?> getActualClass() {
        return actualType instanceof Class ? (Class<?>) actualType : type;
    }

    public Object parse(McpRequest request, McpRequestContext ctx) throws Exception {
        Object result = getValue(request, ctx);
        boolean empty = result == null || result instanceof CharSequence && ((CharSequence) result).length() == 0;
        result = empty && defaultValueParser != null ? defaultValueParser.parse(request, ctx) : result;
        result = result == null || !convertable ? result : convert(ctx, result);
        if (result == null && required) {
            throw new MissingParameterException(name);
        }
        return wrapper == null ? result : wrapper.convert(result);
    }

    private Object getValue(McpRequest request, McpRequestContext ctx) throws Exception {
        String key = getKey();
        switch (location) {
            case QUERY:
                return unary(request.getQuery(key), false);
            case HEADER:
                return unary(request.getHeader(key), false);
            case COOKIE:
                return unary(request.getCookie(key), true);
            case PATH:
                return request.getPath(key);
            case BODY:
                return request.getBody(key);
            case SYSTEM:
                return systemParser.parse(request, ctx);
            default:
                return unary(request.getQuery(key), false);
        }
    }

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

    private boolean isMapType() {
        return Map.class.isAssignableFrom(type);
    }

    private boolean isSimpleType(Class<?> type) {
        return ClassUtils.isSimpleValueType(type);
    }

    public static McpToolParameterBuilder builder() {
        return new McpToolParameterBuilder();
    }

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
        private McpRequestParser systemParser;
        private McpRequestParser defaultValueParser;

        public McpToolParameterBuilder parameter(Parameter parameter) {
            this.parameter = parameter;
            this.name = parameter == null ? null : parameter.getName();
            this.type = parameter == null ? null : parameter.getType();
            this.genericType = parameter == null ? null : parameter.getParameterizedType();
            this.actualType = genericType;
            return this;
        }

        public McpToolParameterBuilder name(String name) {
            this.name = name;
            return this;
        }

        public McpToolParameterBuilder index(int index) {
            this.index = index;
            return this;
        }

        public McpToolParameterBuilder actualType(Type actualType) {
            this.actualType = actualType;
            return this;
        }

        public McpToolParameterBuilder arg(String arg) {
            this.arg = arg;
            return this;
        }

        public McpToolParameterBuilder required(boolean required) {
            this.required = required;
            return this;
        }

        public McpToolParameterBuilder optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public McpToolParameterBuilder convertable(boolean convertable) {
            this.convertable = convertable;
            return this;
        }

        public McpToolParameterBuilder converter(Converter<Object, Object> converter) {
            if (converter != null) {
                this.converter = this.converter == null ? converter : this.converter.then(converter);
            }
            return this;
        }

        public McpToolParameterBuilder wrapper(Converter<Object, Object> wrapper) {
            if (wrapper != null) {
                this.wrapper = this.wrapper == null ? wrapper : this.wrapper.then(wrapper);
            }
            return this;
        }

        public McpToolParameterBuilder location(Location location) {
            this.location = location;
            return this;
        }

        public McpToolParameterBuilder systemParser(McpRequestParser parser) {
            this.systemParser = parser;
            return this;
        }

        public McpToolParameterBuilder defaultValueParser(McpRequestParser defaultValueParser) {
            this.defaultValueParser = defaultValueParser;
            return this;
        }

        public Parameter parameter() {
            return this.parameter;
        }

        public String name() {
            return name;
        }

        public int index() {
            return this.index;
        }

        public Class<?> type() {
            return this.type;
        }

        public Type genericType() {
            return this.genericType;
        }

        public Type actualType() {
            return this.actualType;
        }

        public String arg() {
            return this.arg;
        }

        public String key() {
            if (arg == null || arg.isEmpty()) {
                return name;
            }
            return arg;
        }

        public boolean required() {
            return this.required;
        }

        public boolean optional() {
            return this.optional;
        }

        public Location location() {
            return this.location;
        }

        public boolean isConvertable() {
            return convertable;
        }

        public Converter<Object, Object> converter() {
            return this.converter;
        }

        public Converter<Object, Object> wrapper() {
            return this.wrapper;
        }

        public McpRequestParser systemParser() {
            return this.systemParser;
        }

        public McpRequestParser defaultValueParser() {
            return this.defaultValueParser;
        }

        public Class<?> actualClass() {
            return actualType instanceof Class ? (Class<?>) actualType : type;
        }

        public boolean isType(Class<?> targetClass) {
            return type == targetClass || actualType == targetClass;
        }

        public boolean isAssignableTo(Class<?> targetClass) {
            return targetClass.isAssignableFrom(type) || actualType instanceof Class<?> && ((Class<?>) actualType).isAssignableFrom(targetClass);
        }

        public boolean isSimpleType() {
            return ClassUtils.isSimpleValueType(actualClass());
        }

        public boolean isEntityType() {
            return ClassUtils.isEntity(actualClass());
        }

        public McpToolParameter build() {
            return new McpToolParameter(parameter, index, actualType, arg, required, optional, location,
                    convertable, converter, wrapper, systemParser, defaultValueParser);
        }
    }

    /**
     * Parameter source locations.
     */
    public enum Location {
        /**
         * Query string parameters.
         */
        QUERY,
        /**
         * URL path variables.
         */
        PATH,
        /**
         * HTTP headers.
         */
        HEADER,
        /**
         * HTTP cookies.
         */
        COOKIE,
        /**
         * Request body.
         */
        BODY,
        /**
         * System-generated parameters.
         */
        SYSTEM
    }

}
