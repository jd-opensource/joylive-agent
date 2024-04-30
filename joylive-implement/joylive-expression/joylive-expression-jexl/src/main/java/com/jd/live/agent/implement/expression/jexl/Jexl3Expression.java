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

import com.jd.live.agent.core.util.expression.Expression;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;

import java.util.Map;

public class Jexl3Expression implements Expression {

    private final JexlExpression expression;

    public Jexl3Expression(JexlExpression expression) {
        this.expression = expression;
    }

    @Override
    public Object evaluate(Map<String, Object> context) {
        return expression.evaluate(new MapContext(context));
    }
}
