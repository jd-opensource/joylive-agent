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

import com.jd.live.agent.core.parser.ObjectConverter;
import com.jd.live.agent.governance.jsonrpc.JsonRpcError;
import com.jd.live.agent.governance.jsonrpc.JsonRpcException;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Default implementation of McpParameterConverter that handles parameter conversion for JSON-RPC calls.
 */
public class DefaultMcpParameterConverter implements McpParameterConverter {

    public static final McpParameterConverter INSTANCE = new DefaultMcpParameterConverter();

    private static final Set<Class<?>> SIMPLE_TYPES = new HashSet<>(Arrays.asList(
            boolean.class,
            char.class,
            byte.class,
            short.class,
            int.class,
            long.class,
            float.class,
            double.class,
            Boolean.class,
            Character.class,
            Byte.class,
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            String.class
    ));

    @Override
    public Object[] convert(McpToolMethod method, Object params, ObjectConverter converter) {
        McpToolParameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            return new Object[0];
        }
        if (params instanceof Map) {
            return parse(parameters, (Map<?, ?>) params, converter);
        } else if (params instanceof List) {
            return parse(parameters, (List<Object>) params, converter);
        } else if (params instanceof Object[]) {
            return parse(parameters, Arrays.asList((Object[]) params), converter);
        } else {
            return new Object[]{convert(params, parameters[0].getType(), parameters[0].getType(), converter)};
        }
    }

    /**
     * Converts named parameters from a map to method arguments.
     *
     * @param parameters Method parameter definitions
     * @param params     Map of parameter names to values
     * @param converter  Object converter to use
     * @return Converted parameter array
     */
    private Object[] parse(McpToolParameter[] parameters, Map<?, ?> params, ObjectConverter converter) {
        return parse(parameters, params, (p, i) -> params.get(p.getName()), converter);
    }

    /**
     * Converts positional parameters from a list to method arguments.
     *
     * @param parameters Method parameter definitions
     * @param params     List of parameter values
     * @param converter  Object converter to use
     * @return Converted parameter array
     */
    private Object[] parse(McpToolParameter[] parameters, List<Object> params, ObjectConverter converter) {
        return parse(parameters, params, (p, i) -> params.get(i), converter);
    }

    /**
     * Converts positional parameters from a list to method arguments.
     *
     * @param parameters Method parameter definitions
     * @param params     List of parameter values
     * @param converter  Object converter to use
     * @return Converted parameter array
     */
    private Object[] parse(McpToolParameter[] parameters,
                           Object params,
                           BiFunction<McpToolParameter, Integer, Object> paramFunc,
                           ObjectConverter converter) {
        Object[] args = new Object[parameters.length];
        if (parameters.length == 1) {
            args[0] = parse(parameters[0], converter, p -> params);
        } else {
            AtomicInteger counter = new AtomicInteger(0);
            for (int i = 0; i < parameters.length; i++) {
                args[i] = parse(parameters[i], converter, p -> paramFunc.apply(p, counter.getAndIncrement()));
                if (args[i] == null && parameters[i].isRequired()) {
                    throw new JsonRpcException("Required parameter at position " + i + " is missing", JsonRpcError.INVALID_PARAMS);
                }
            }
        }
        return args;
    }

    private Object parse(McpToolParameter parameter, ObjectConverter converter, Function<McpToolParameter, Object> func) {
        ParameterParser parser = parameter.getParser();
        if (parser != null) {
            Object result = parser.parse();
            if (result != null) {
                return parser.isConvertable()
                        ? parameter.convert(convert(result, parameter.getType(), parameter.getGenericType(), converter))
                        : parameter.convert(result);
            } else if (result == null && !parser.isOptional()) {
                return parameter.convert(null);
            }
        }
        return parameter.convert(convert(func.apply(parameter), parameter.getType(), parameter.getGenericType(), converter));
    }

    /**
     * Converts a single value to target type using converter.
     *
     * @param value       Value to convert
     * @param targetClass Target class type
     * @param targetType  Target generic type
     * @param converter   Object converter to use
     * @return Converted value
     */
    private Object convert(Object value, Class<?> targetClass, Type targetType, ObjectConverter converter) {
        if (value == null) {
            return null;
        }
        try {
            if (targetClass.isInstance(value) && (SIMPLE_TYPES.contains(targetClass) || targetClass.isEnum())) {
                return value;
            }
            return converter.convert(value, targetType);
        } catch (Exception e) {
            throw new JsonRpcException("Failed to convert value to " + targetClass.getName(), e, JsonRpcError.INVALID_PARAMS);
        }
    }
}
