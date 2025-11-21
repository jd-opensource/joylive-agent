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

import com.jd.live.agent.core.mcp.converter.OpenApiConverter;
import com.jd.live.agent.core.mcp.spec.v1.ListToolsResult;
import com.jd.live.agent.core.mcp.spec.v1.Tool;
import com.jd.live.agent.core.mcp.McpRequestContext;

import java.util.List;

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
        OpenApiConverter converter = new OpenApiConverter(ctx.getOpenApi().get(), ctx.getVersion());
        List<Tool> tools = converter.convert();
        return new ListToolsResult(tools);
    }

}
