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

/**
 * Factory for parsing and evaluating expressions.
 */
public interface ExpressionFactory {

    /**
     * Parses a string into an Expression object.
     *
     * @param expression the expression string to parse
     * @return the parsed Expression
     */
    Expression parse(String expression);

    /**
     * Evaluates an Expression to its result value.
     *
     * @param expression the Expression to evaluate
     * @return the evaluation result
     */
    Object evaluate(Expression expression);

}
