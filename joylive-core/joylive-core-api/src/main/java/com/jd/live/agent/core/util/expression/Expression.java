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

import java.util.Map;

/**
 * This interface defines an abstract method for evaluating expressions. Implementing classes are expected to provide
 * logic to evaluate the expression based on the provided context. The context is a {@link Map} that maps variable names
 * to their respective values, which can be used during the evaluation of the expression.
 */
public interface Expression {

    /**
     * Evaluates the expression based on the given context.
     * The context is a map containing variable names mapped to their values, which can be utilized in the evaluation.
     *
     * @param context A map containing variables and their values to be used in the expression evaluation.
     * @return The result of the expression evaluation. The type of the result can be any object, depending on the
     * implementation of the expression and the given context.
     */
    Object evaluate(Map<String, Object> context);

}
