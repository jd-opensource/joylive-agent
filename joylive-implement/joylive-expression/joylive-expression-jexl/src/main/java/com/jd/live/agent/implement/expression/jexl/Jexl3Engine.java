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
package com.jd.live.agent.implement.expression.jexl;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.util.expression.Expression;
import com.jd.live.agent.core.util.expression.ExpressionEngine;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Extension(value = "jexl", order = ExpressionEngine.ORDER_JEXL)
public class Jexl3Engine implements ExpressionEngine {

    private final JexlEngine engine;

    private final Map<String, Expression> expressions = new ConcurrentHashMap<>();

    public Jexl3Engine() {
        engine = configure(new JexlBuilder()).create();
    }

    protected JexlBuilder configure(JexlBuilder builder) {
        return builder;
    }

    @Override
    public Expression build(final String expression) {
        return expression == null || expression.isEmpty() ? null : expressions.computeIfAbsent(expression,
                v -> new Jexl3Expression(engine.createExpression(v)));
    }

}
