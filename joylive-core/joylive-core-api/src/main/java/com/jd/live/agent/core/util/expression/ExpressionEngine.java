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
package com.jd.live.agent.core.util.expression;

import com.jd.live.agent.core.extension.annotation.Extensible;

/**
 * Represents an expression engine that is capable of building {@link Expression} objects from string representations.
 * This interface is marked with {@code @Extensible("ExpressionEngine")} indicating that it can be extended or implemented
 * by multiple expression engines, each providing their own mechanism for parsing and building expressions.
 */
@Extensible("ExpressionEngine")
public interface ExpressionEngine {

    /**
     * The default order value for JEXL (Java Expression Language) based implementations.
     * This can be used to prioritize different expression engines when multiple engines are available.
     */
    int ORDER_JEXL = 100;

    /**
     * Builds an {@link Expression} object from the given string representation of the expression.
     * This method is responsible for parsing the string and constructing an executable expression object.
     *
     * @param expression The string representation of the expression to be built.
     * @return An {@link Expression} object that can be evaluated with a context.
     */
    Expression build(String expression);

}
