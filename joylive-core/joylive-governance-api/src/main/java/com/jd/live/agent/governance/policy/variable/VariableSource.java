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

import com.jd.live.agent.governance.policy.HttpScope;
import lombok.Getter;

/**
 * Represents a source of variables.
 */
public interface VariableSource {

    /**
     * Represents a source of HTTP variables.
     */
    interface HttpVariableSource extends VariableSource {

        /**
         * Gets the scope of the HTTP variable.
         *
         * @return The scope of the HTTP variable.
         */
        HttpScope getScope();

        /**
         * Gets the key of the HTTP variable.
         *
         * @return The key of the HTTP variable.
         */
        String getKey();
    }

    /**
     * Represents a source of method variables.
     */
    interface MethodVariableSource extends VariableSource {

        /**
         * Gets the argument index of the method variable.
         *
         * @return The argument index.
         */
        int getArgument();

        /**
         * Gets the path associated with the method variable.
         *
         * @return The path.
         */
        String getPath();

    }

    /**
     * Represents a source of expression variables.
     */
    interface ExpressionVariableSource extends VariableSource {

        /**
         * Gets the expression of the variable.
         *
         * @return The expression.
         */
        String getExpression();

    }

    /**
     * Represents an HTTP variable.
     */
    @Getter
    class HttpVariable implements HttpVariableSource {

        /**
         * The scope of the HTTP variable.
         */
        private final HttpScope scope;

        /**
         * The key of the HTTP variable.
         */
        private final String key;

        /**
         * Constructs a new {@code HttpVariable}.
         *
         * @param scope The scope of the HTTP variable.
         * @param key   The key of the HTTP variable.
         */
        public HttpVariable(HttpScope scope, String key) {
            this.scope = scope;
            this.key = key;
        }

    }

    /**
     * Represents a method variable.
     */
    class MethodVariable implements MethodVariableSource {

        /**
         * The argument index of the method variable.
         */
        private final int argument;

        /**
         * The path associated with the method variable.
         */
        private final String path;

        /**
         * Constructs a new {@code MethodVariable}.
         *
         * @param argument The argument index of the method variable.
         * @param path     The path associated with the method variable.
         */
        public MethodVariable(int argument, String path) {
            this.argument = argument;
            this.path = path;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getArgument() {
            return argument;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getPath() {
            return path;
        }
    }

    /**
     * Represents an expression variable.
     */
    class ExpressionVariable implements ExpressionVariableSource {

        /**
         * The expression of the variable.
         */
        private final String expression;

        /**
         * Constructs a new {@code ExpressionVariable}.
         *
         * @param expression The expression of the variable.
         */
        public ExpressionVariable(String expression) {
            this.expression = expression;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getExpression() {
            return expression;
        }
    }
}

