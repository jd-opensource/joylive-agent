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
package com.jd.live.agent.core.mcp.handler.builder;

import com.jd.live.agent.core.mcp.McpRequestContext;
import com.jd.live.agent.core.mcp.McpToolMethod;
import com.jd.live.agent.core.mcp.converter.OpenApiConverter;
import com.jd.live.agent.core.mcp.spec.v1.ListToolsResult;
import com.jd.live.agent.core.mcp.spec.v1.Tool;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
        OpenApiConverter converter = new OpenApiConverter(ctx.getOpenApi(), ctx.getVersion());
        List<Tool> tools = converter.convert((path, item) -> {
            List<McpToolMethod> methods = ctx.getToolMethodsByPath(path);
            if (methods == null || methods.isEmpty()) {
                return null;
            }
            // multi http method may refer to the same method
            // @RequestMapping(value = "/status/{code}", method = {RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST})
            Map<String, AtomicInteger> targets = getMethodRef(methods);
            return item.operations((m, o) -> {
                AtomicInteger ref = targets.get(m);
                if (ref == null) {
                    return false;
                } else if (ref.get() > 0) {
                    // keep only one operation for the same method
                    ref.set(0);
                    return true;
                }
                return false;
            });
        });
        return new ListToolsResult(tools);
    }

    private Map<String, AtomicInteger> getMethodRef(List<McpToolMethod> methods) {
        Map<Method, AtomicInteger> refs = new HashMap<>(methods.size());
        Map<String, AtomicInteger> targets = new HashMap<>(8);
        for (McpToolMethod method : methods) {
            if (method.getHttpMethods() != null) {
                AtomicInteger ref = refs.computeIfAbsent(method.getMethod(), m -> new AtomicInteger());
                for (String httpMethod : method.getHttpMethods()) {
                    ref.getAndIncrement();
                    targets.put(httpMethod, ref);
                }
            }
        }
        return targets;
    }

}
