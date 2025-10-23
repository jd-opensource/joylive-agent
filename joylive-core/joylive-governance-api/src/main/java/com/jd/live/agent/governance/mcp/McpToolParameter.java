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

import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a parameter definition for MCP tool.
 */
public class McpToolParameter {

    private final String name;

    private final int index;

    private final Class<?> type;

    private final Type genericType;

    private final boolean required;

    private final Supplier<Object> supplier;

    private final Function<Object, Object> converter;

    public McpToolParameter(String name, int index, Class<?> type, Type genericType, boolean required) {
        this(name, index, type, genericType, required, null, null);
    }

    public McpToolParameter(String name, int index, Class<?> type, Type genericType, boolean required, Supplier<Object> supplier) {
        this(name, index, type, genericType, required, null, supplier);
    }

    public McpToolParameter(String name, int index, Class<?> type, Type genericType, boolean required, Function<Object, Object> converter, Supplier<Object> supplier) {
        this.name = name;
        this.index = index;
        this.type = type;
        this.genericType = genericType;
        this.required = required;
        this.converter = converter;
        this.supplier = supplier;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public Class<?> getType() {
        return type;
    }

    public Type getGenericType() {
        return genericType;
    }

    public boolean isRequired() {
        return required;
    }

    public Function<Object, Object> getConverter() {
        return converter;
    }

    public Supplier<Object> getSupplier() {
        return supplier;
    }

    public boolean isSystem() {
        return supplier != null;
    }

    public Object getValue() {
        return supplier == null ? null : supplier.get();
    }

    public Object convert(Object value) {
        return value == null || converter == null ? value : converter.apply(value);
    }

}
