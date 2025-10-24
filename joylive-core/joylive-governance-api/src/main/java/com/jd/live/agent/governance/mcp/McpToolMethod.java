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

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Represents a method definition for MCP tool.
 */
@Getter
public class McpToolMethod {

    public static Method HANDLE_METHOD;

    private String name;

    private Object controller;

    private Method method;

    private McpToolParameter[] parameters;

    private Set<String> paths;

    @Builder
    public McpToolMethod(String name, Object controller, Method method, McpToolParameter[] parameters, Set<String> paths) {
        this.name = name;
        this.controller = controller;
        this.method = method;
        this.parameters = parameters;
        this.paths = paths;
    }
}
