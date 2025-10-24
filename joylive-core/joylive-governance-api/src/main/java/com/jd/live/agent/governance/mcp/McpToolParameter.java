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

import lombok.Builder;
import lombok.Getter;

import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * Represents a parameter definition for MCP tool.
 */
@Getter
public class McpToolParameter {

    private final String name;

    private final int index;

    private final Class<?> type;

    private final Type genericType;

    private final boolean required;

    private final ParameterParser parser;

    private final Function<Object, Object> converter;

    @Builder
    public McpToolParameter(String name,
                            int index,
                            Class<?> type,
                            Type genericType,
                            boolean required,
                            Function<Object, Object> converter,
                            ParameterParser parser) {
        this.name = name;
        this.index = index;
        this.type = type;
        this.genericType = genericType;
        this.required = required;
        this.converter = converter;
        this.parser = parser;
    }

    public Object convert(Object value) {
        return value == null || converter == null ? value : converter.apply(value);
    }

}
