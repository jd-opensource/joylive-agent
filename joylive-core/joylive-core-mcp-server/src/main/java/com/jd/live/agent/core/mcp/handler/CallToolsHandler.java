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
package com.jd.live.agent.core.mcp.handler;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.mcp.*;
import com.jd.live.agent.core.mcp.McpToolParameter.Location;
import com.jd.live.agent.core.mcp.exception.McpException;
import com.jd.live.agent.core.mcp.spec.v1.*;
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.core.util.map.CaseInsensitiveLinkedMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Handler for processing MCP tool call requests.
 * Implements the JSON-RPC method for tool execution.
 */
@Extension(JsonRpcMessage.METHOD_TOOLS_CALL)
public class CallToolsHandler implements McpHandler {

    @Override
    public JsonRpcResponse handle(JsonRpcRequest request, McpRequestContext ctx) throws McpException {
        CallToolRequest req = ctx.convert(request.getParams(), CallToolRequest.class);
        McpToolMethod method = ctx.getToolMethodByName(req.getName());
        if (method == null) {
            return JsonRpcResponse.createMethodNotFoundResponse(request.getId());
        }
        try {
            McpRequest mcpRequest = new McpCallToolRequest(ctx, method, req.getArguments());
            Object[] args = parseArgs(method, mcpRequest, ctx);
            method.validate(args);
            McpToolInterceptor interceptor = ctx.getInterceptor();
            McpToolInvocation invocation = new McpToolInvocation(mcpRequest, ctx.getSession(), method, args);
            Object result = interceptor == null ? invocation.call() : interceptor.intercept(invocation);
            result = ctx.getVersion().output(result);
            CallToolResult response = CallToolResult.builder().structuredContent(result).build();
            return JsonRpcResponse.createSuccessResponse(request.getId(), response);
        } catch (Throwable e) {
            // Tool Execution Errors: Reported in tool results with isError: true
            return JsonRpcResponse.createSuccessResponse(request.getId(), new CallToolResult(e));
        }
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

    /**
     * Adapter class that converts MCP call tool requests to standard MCP call requests.
     */
    private static class McpCallToolRequest implements McpRequest {
        private final McpRequestContext ctx;
        private final McpToolMethod method;
        private final Map<String, Object> arguments;
        private final LazyObject<Map<String, ? extends Object>> queries;
        private final LazyObject<Map<String, ? extends Object>> headers;
        private final LazyObject<Map<String, ? extends Object>> cookies;
        private final LazyObject<Map<String, ? extends Object>> paths;

        McpCallToolRequest(McpRequestContext ctx, McpToolMethod method, Map<String, Object> arguments) {
            this.ctx = ctx;
            this.method = method;
            this.arguments = arguments;
            this.queries = new LazyObject<>(() -> getParams(Location.QUERY, i -> new LinkedHashMap<>()));
            this.headers = new LazyObject<>(() -> merge(ctx.getHeaders(), Location.HEADER, i -> new CaseInsensitiveLinkedMap<>()));
            this.cookies = new LazyObject<>(() -> merge(ctx.getCookies(), Location.COOKIE, i -> new LinkedHashMap<>()));
            this.paths = new LazyObject<>(() -> getParams(Location.PATH, i -> new LinkedHashMap<>()));
        }

        @Override
        public String getRemoteAddr() {
            return ctx.getRemoteAddr();
        }

        @Override
        public Map<String, ? extends Object> getQueries() {
            return queries.get();
        }

        @Override
        public Map<String, ? extends Object> getHeaders() {
            return headers.get();
        }

        @Override
        public Object getHeader(String name) {
            if (name == null || name.isEmpty()) {
                return null;
            }
            Map<String, ? extends Object> map = headers.get();
            Object result = map == null ? null : map.get(name);
            if (result == null) {
                result = ctx.getHeader(name);
            }
            return result;
        }

        @Override
        public Map<String, ? extends Object> getCookies() {
            return cookies.get();
        }

        @Override
        public Object getCookie(String name) {
            return ctx.getCookie(name);
        }

        @Override
        public Map<String, ? extends Object> getPaths() {
            return paths.get();
        }

        @Override
        public Object getBody(String name) {
            return arguments == null ? null : arguments.get("body");
        }

        /**
         * Merges two maps, preserving entries from destination map when keys conflict.
         *
         * @param src      Source map to merge from
         * @param location Parameter location identifier
         * @param function Function to create destination map based on size
         * @return Merged map or original source/destination if either is empty
         */
        private Map<String, ? extends Object> merge(Map<String, ?> src, Location location, Function<Integer, Map<String, Object>> function) {
            Map<String, Object> dest = getParams(location, function);
            if (dest == null || dest.isEmpty()) {
                return src;
            } else if (src == null || src.isEmpty()) {
                return dest;
            }
            for (Map.Entry<String, ?> entry : src.entrySet()) {
                dest.putIfAbsent(entry.getKey(), entry.getValue());
            }
            return dest;
        }

        /**
         * Retrieves parameters for the specified location using the provided map factory function.
         *
         * @param location Parameter location identifier
         * @param function Function to create a map of appropriate size
         * @return Map of parameters or null if no parameters exist
         */
        private Map<String, Object> getParams(Location location, Function<Integer, Map<String, Object>> function) {
            if (arguments == null || arguments.isEmpty() || method.size() == 0) {
                return null;
            }
            Map<String, Object> result = function.apply(arguments.size());
            method.parameter(location, (arg, parameter) -> result.put(arg, arguments.get(arg)));
            return result;
        }
    }
}
