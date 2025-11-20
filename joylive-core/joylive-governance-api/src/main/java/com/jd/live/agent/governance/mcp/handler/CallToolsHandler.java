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
package com.jd.live.agent.governance.mcp.handler;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.mcp.McpRequest;
import com.jd.live.agent.governance.mcp.McpRequestContext;
import com.jd.live.agent.governance.mcp.McpToolMethod;
import com.jd.live.agent.governance.mcp.McpToolParameter;
import com.jd.live.agent.governance.mcp.spec.*;

import java.util.Map;

@Extension(JsonRpcMessage.METHOD_TOOLS_CALL)
public class CallToolsHandler implements McpHandler {

    @Override
    public JsonRpcResponse handle(JsonRpcRequest request, McpRequestContext ctx) throws Exception {
        CallToolRequest req = ctx.convert(request.getParams(), CallToolRequest.class);
        McpToolMethod method = ctx.getToolMethodByName(req.getName());
        if (method == null) {
            return JsonRpcResponse.createMethodNotFoundResponse(request.getId());
        }

        Object[] args = parseArgs(method, createRequest(method, req, ctx), ctx);
        Object result = method.getMethod().invoke(method.getController(), args);
        result = ctx.getVersion().output(result);
        CallToolResult response = CallToolResult.builder().isError(false).structuredContent(result).build();
        return JsonRpcResponse.createSuccessResponse(request.getId(), response);
    }

    private McpRequest createRequest(McpToolMethod method, CallToolRequest request, McpRequestContext ctx) {
        Map<String, Object> arguments = request.getArguments();
        // TODO filter headers & cookies & paths parameters
        //  and fallback to ctx.getHeaders or ctx.getCookies.
        return new McpRequest() {
            @Override
            public Map<String, ? extends Object> getQueries() {
                return arguments;
            }

            @Override
            public Map<String, ? extends Object> getHeaders() {
                return ctx.getHeaders();
            }

            @Override
            public Object getHeader(String name) {
                return ctx.getHeader(name);
            }

            @Override
            public Map<String, ? extends Object> getCookies() {
                return ctx.getCookies();
            }

            @Override
            public Object getCookie(String name) {
                return ctx.getCookie(name);
            }

            @Override
            public Map<String, Object> getPaths() {
                return arguments;
            }

            @Override
            public Object getBody(String name) {
                // TODO fix name
                return arguments == null ? null : arguments.get("body");
            }
        };
    }

    /**
     * Parse input to method parameters.
     *
     * @param method  Target method
     * @param request MCP request params
     * @param ctx     The context containing conversion and parsing utilities for the request
     * @return Converted parameter array
     * @throws Exception If parsing fails
     */
    private Object[] parseArgs(McpToolMethod method, McpRequest request, McpRequestContext ctx) throws Exception {
        McpToolParameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            return new Object[0];
        }
        Object[] args = new Object[parameters.length];
        if (parameters.length == 1) {
            args[0] = parameters[0].parse(request, ctx);
        } else {
            int i = 0;
            for (McpToolParameter parameter : parameters) {
                args[i++] = parameter.parse(request, ctx);
            }
        }
        return args;
    }
}
