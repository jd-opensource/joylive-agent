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

import com.jd.live.agent.core.mcp.McpToolParameter.Location;
import lombok.Builder;
import lombok.Getter;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Represents a method definition for MCP tool.
 */
@Getter
public class McpToolMethod {

    public static Method HANDLE_METHOD;

    private final String name;

    private final Object controller;

    private final Method method;

    private final McpToolParameter[] parameters;

    private final Set<String> paths;

    private final Set<String> httpMethods;

    // TODO String[] params()
    // TODO String[] headers()
    // TODO String version() 1.7/1.7+

    @Builder
    public McpToolMethod(String name,
                         Object controller,
                         Method method,
                         McpToolParameter[] parameters,
                         Set<String> paths,
                         Set<String> httpMethods) {
        this.name = name;
        this.controller = controller;
        this.method = method;
        this.parameters = parameters;
        this.paths = paths;
        this.httpMethods = httpMethods;
    }

    public Object invoke(Object... args) throws Throwable {
        return method.invoke(controller, args);
    }

    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    public Type getGenericReturnType() {
        return method.getGenericReturnType();
    }

    public int size() {
        return parameters == null ? 0 : parameters.length;
    }

    public McpToolParameter[] getParameters(Location location) {
        if (location == null || parameters == null || parameters.length == 0) {
            return null;
        }
        if (parameters.length == 1) {
            return parameters[0].getLocation() == location ? new McpToolParameter[]{parameters[0]} : null;
        }
        List<McpToolParameter> result = new ArrayList<>(parameters.length);
        for (McpToolParameter parameter : parameters) {
            if (parameter.getLocation() == location) {
                result.add(parameter);
            }
        }
        return result.isEmpty() ? null : result.toArray(new McpToolParameter[result.size()]);
    }

    public void parameter(Location location, BiConsumer<String, McpToolParameter> consumer) {
        if (location == null || consumer == null || parameters == null || parameters.length == 0) {
            return;
        }
        if (parameters.length == 1) {
            if (parameters[0].getLocation() == location) {
                consumer.accept(parameters[0].getArg(), parameters[0]);
            }
        } else {
            for (McpToolParameter parameter : parameters) {
                if (parameter.getLocation() == location) {
                    consumer.accept(parameter.getKey(), parameter);
                }
            }
        }
    }
}
