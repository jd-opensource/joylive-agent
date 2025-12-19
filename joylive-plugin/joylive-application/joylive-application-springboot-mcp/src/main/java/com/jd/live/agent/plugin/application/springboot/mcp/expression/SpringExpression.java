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
package com.jd.live.agent.plugin.application.springboot.mcp.expression;

import com.jd.live.agent.core.mcp.Expression;
import lombok.Getter;

/**
 * Spring-specific implementation of Expression interface.
 * Handles Spring property placeholders and SpEL expressions.
 */
@Getter
public class SpringExpression implements Expression {

    /**
     * The expression string
     */
    private String expression;

    /**
     * Flag indicating if this is a literal expression (no placeholders or SpEL)
     */
    private boolean literal;

    /**
     * Creates a new Spring expression
     *
     * @param expression The expression string to evaluate
     */
    public SpringExpression(String expression) {
        this.expression = expression;
        this.literal = expression == null || !expression.contains("${") && !expression.contains("#{");
    }

    /**
     * Returns the expression string
     *
     * @return The expression string
     */
    public String toString() {
        return expression;
    }

}
