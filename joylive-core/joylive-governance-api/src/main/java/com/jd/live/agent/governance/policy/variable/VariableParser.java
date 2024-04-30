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
package com.jd.live.agent.governance.policy.variable;

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.request.ServiceRequest;

/**
 * The {@code VariableParser} interface defines the contract for parsing variables from various sources within a service request.
 * Implementations of this interface can parse variables based on different types such as method invocations, HTTP parameters,
 * or expressions.
 * <p>
 * This interface is designed to be extensible, allowing for custom implementations that can handle additional types
 * or parsing strategies. It supports generic types {@code T} and {@code S} to allow for flexibility in the types of requests
 * and variable sources it can work with.
 * <p>
 * The {@code parse} method is the core method that needs to be implemented, which takes a request and a source, returning
 * the parsed variable as a {@code String}. There is also a default implementation of the {@code parse} method that accepts
 * a {@code VariableFunction} to further process the parsed variable.
 *
 * @param <T> The type of the service request from which variables are to be parsed.
 * @param <S> The type of the variable source that provides the variables to be parsed.
 * @see ServiceRequest
 * @see VariableSource
 * @see VariableFunction
 */
@Extensible("VariableParser")
public interface VariableParser<T extends ServiceRequest, S extends VariableSource> {

    String TYPE_METHOD = "method";

    String TYPE_HTTP = "http";

    String TYPE_EXPRESSION = "expression";

    /**
     * Parses a variable from the given request and source.
     *
     * @param request The service request from which the variable is to be parsed.
     * @param source  The source that provides the variable.
     * @return The parsed variable as a {@code String}. Returns {@code null} if the variable cannot be parsed.
     */
    String parse(T request, S source);

    /**
     * Parses a variable from the given request and source, then applies a {@code VariableFunction} to the result.
     * This default method first invokes the {@code parse} method to retrieve the variable and then applies the given
     * function if the result is not {@code null} and the function is not {@code null}.
     *
     * @param request  The service request from which the variable is to be parsed.
     * @param source   The source that provides the variable.
     * @param function The function to be applied to the parsed variable.
     * @return The result of applying the {@code VariableFunction} to the parsed variable, or the parsed variable itself
     * if the function is {@code null}. Returns {@code null} if the variable cannot be parsed.
     */
    default String parse(T request, S source, VariableFunction function) {
        String result = parse(request, source);
        return result == null || function == null ? result : function.compute(result);
    }

}

