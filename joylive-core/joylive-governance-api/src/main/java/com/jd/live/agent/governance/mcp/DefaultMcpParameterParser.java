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

import com.jd.live.agent.governance.mcp.spec.ErrorCodes;
import com.jd.live.agent.governance.mcp.spec.JsonRpcException;
import com.jd.live.agent.governance.mcp.spec.JsonRpcException.NotEnoughParameter;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

/**
 * Default implementation of McpParameterConverter that handles parameter conversion for JSON-RPC calls.
 */
public class DefaultMcpParameterParser implements McpParameterParser {

    public static final McpParameterParser INSTANCE = new DefaultMcpParameterParser();

    @Override
    public Object[] parse(McpToolMethod method, Object params, McpRequestContext ctx) throws Exception {
        McpToolParameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            return new Object[0];
        } else if (parameters.length == 1) {
            return parse(parameters, params, ctx, (p, i) -> params);
        } else if (params instanceof Map) {
            return parseMap(parameters, (Map<?, ?>) params, ctx);
        } else if (params instanceof List) {
            return parseList(parameters, (List<?>) params, ctx);
        } else if (params instanceof Object[]) {
            return parseArray(parameters, (Object[]) params, ctx);
        } else if (params.getClass().isArray()) {
            return parseArrayType(parameters, params, ctx);
        } else {
            return parse(parameters, params, ctx, (p, i) -> params);
        }
    }

    /**
     * Parse parameters from map using parameter names as keys
     *
     * @param parameters Parameter definitions
     * @param params     Map of parameter name to value
     * @param ctx        Request context
     * @return Parsed parameter array
     * @throws Exception if parsing fails
     */
    private Object[] parseMap(McpToolParameter[] parameters, Map<?, ?> params, McpRequestContext ctx) throws Exception {
        return parse(parameters, params, ctx, (p, i) -> params.get(p.getName()));
    }

    /**
     * Parse parameters from array type using reflection
     *
     * @param parameters Parameter definitions
     * @param params Array object to parse
     * @param ctx Request context
     * @return Parsed parameter array
     * @throws Exception if parsing fails
     */
    private Object[] parseArrayType(McpToolParameter[] parameters, Object params, McpRequestContext ctx) throws Exception {
        int length = Array.getLength(params);
        return parse(parameters, params, ctx, (p, i) -> {
            if (i >= length) {
                throw new NotEnoughParameter();
            }
            return Array.get(params, i);
        });
    }

    /**
     * Parse parameters from object array
     *
     * @param parameters Parameter definitions
     * @param params     Array of parameter values
     * @param ctx        Request context
     * @return Parsed parameter array
     * @throws Exception if parsing fails
     */
    private Object[] parseArray(McpToolParameter[] parameters, Object[] params, McpRequestContext ctx) throws Exception {
        return parse(parameters, params, ctx, (p, i) -> {
            if (i >= params.length) {
                throw new NotEnoughParameter();
            }
            return params[i];
        });
    }

    /**
     * Parse parameters from list
     *
     * @param parameters Parameter definitions
     * @param objects List of parameter values
     * @param ctx Request context
     * @return Parsed parameter array
     * @throws Exception if parsing fails
     */
    private Object[] parseList(McpToolParameter[] parameters, List<?> objects, McpRequestContext ctx) throws Exception {
        return parse(parameters, objects, ctx, (p, i) -> {
            if (i >= objects.size()) {
                throw new NotEnoughParameter();
            }
            return objects.get(i);
        });
    }

    /**
     * Parses MCP request parameters into method arguments.
     *
     * <p>Handles both single and multiple parameter cases. For multiple parameters,
     * applies the paramFunc to extract individual values in sequence.
     *
     * @param parameters the parameter definitions to parse
     * @param params the raw parameter values
     * @param ctx the request context for conversion
     * @param paramFunc function to extract parameter value at specific position
     * @return array of parsed argument objects
     * @throws JsonRpcException if a required parameter is missing
     * @throws Exception if parsing fails
     */
    private Object[] parse(McpToolParameter[] parameters,
                           Object params,
                           McpRequestContext ctx,
                           BiFunction<McpToolParameter, Integer, Object> paramFunc) throws Exception {
        Object[] args = new Object[parameters.length];
        if (parameters.length == 1) {
            args[0] = parameters[0].parse(ctx, p -> params);
        } else {
            AtomicInteger counter = new AtomicInteger(0);
            for (int i = 0; i < parameters.length; i++) {
                args[i] = parameters[i].parse(ctx, p -> paramFunc.apply(p, counter.getAndIncrement()));
                if (args[i] == null && parameters[i].isRequired()) {
                    throw new JsonRpcException("Required parameter at position " + i + " is missing", ErrorCodes.INVALID_PARAMS);
                }
            }
        }
        return args;
    }
}
