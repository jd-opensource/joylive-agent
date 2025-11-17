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

import com.jd.live.agent.governance.mcp.McpRequestContext;
import com.jd.live.agent.governance.mcp.spec.ListToolsResult;

/**
 * Builder interface for creating tool listing results.
 */
public interface ListToolsBuilder {

    /**
     * Creates a result containing available tools based on the given context.
     *
     * @param ctx The MCP request context
     * @return The list tools result
     */
    ListToolsResult create(McpRequestContext ctx);

}
