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
package com.jd.live.agent.governance.mcp;

import com.jd.live.agent.core.util.converter.Converter;
import lombok.Getter;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.function.Function;

import static com.jd.live.agent.core.util.type.ClassUtils.SIMPLE_TYPES;

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

    private final boolean convertable;

    private final Converter<Object, ?> converter;

    private final McpRequestParser parser;

    private final McpRequestParser defaultValueParser;

    public McpToolParameter(Parameter parameter,
                            int index,
                            Type actualType,
                            String arg,
                            boolean required,
                            boolean optional,
                            boolean convertable,
                            Converter<Object, ?> converter,
                            McpRequestParser parser,
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
        this.convertable = convertable;
        this.converter = converter;
        this.parser = parser;
        this.defaultValueParser = defaultValueParser;
    }

    public String getArg() {
        if (arg == null || arg.isEmpty()) {
            return name;
        }
        return arg;
    }

    public Class<?> getActualClass() {
        return actualType instanceof Class ? (Class<?>) actualType : type;
    }

    public boolean isFramework() {
        return parser != null;
    }

    public Object parse(McpRequestContext ctx, Function<McpToolParameter, Object> valueFunc) throws Exception {
        Object result;
        if (isFramework()) {
            // system parser
            result = parser.parse(ctx);
            result = result == null && defaultValueParser != null ? defaultValueParser.parse(ctx) : result;
            result = convertable ? convert(ctx, result) : result;
        } else {
            result = valueFunc.apply(this);
            result = result == null && defaultValueParser != null ? defaultValueParser.parse(ctx) : result;
            result = convert(ctx, result);
        }
        return wrap(result);
    }

    private Object convert(McpRequestContext ctx, Object target) {
        if (target == null) {
            return null;
        }
        Class<?> resultClass = getActualClass();
        Class<?> targetClass = target.getClass();
        if (resultClass.isInstance(target)) {
            if (SIMPLE_TYPES.contains(targetClass) || targetClass.isEnum()) {
                return wrap(target);
            }
        }
        return ctx.getConverter().convert(target, actualType);
    }

    private Object wrap(Object value) {
        return value == null || converter == null ? value : converter.convert(value);
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
        private boolean convertable;
        private Converter<Object, ?> converter;
        private McpRequestParser parser;
        private McpRequestParser defaultValueParser;

        public McpToolParameterBuilder parameter(Parameter parameter) {
            this.parameter = parameter;
            this.name = parameter == null ? null : parameter.getName();
            this.type = parameter == null ? null : parameter.getType();
            this.genericType = parameter == null ? null : parameter.getParameterizedType();
            this.actualType = genericType;
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

        public McpToolParameterBuilder converter(Converter<Object, ?> converter) {
            this.converter = converter;
            return this;
        }

        public McpToolParameterBuilder parser(McpRequestParser parser) {
            this.parser = parser;
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

        public boolean required() {
            return this.required;
        }

        public boolean optional() {
            return this.optional;
        }

        public boolean convertable() {
            return this.convertable;
        }

        public Converter<Object, ?> converter() {
            return this.converter;
        }

        public McpRequestParser parser() {
            return this.parser;
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

        public McpToolParameter build() {
            return new McpToolParameter(parameter, index, actualType, arg, required, optional, convertable, converter, parser, defaultValueParser);
        }
    }

}
