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
package com.jd.live.agent.governance.policy.variable.expression;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.expression.Expression;
import com.jd.live.agent.core.util.expression.ExpressionEngine;
import com.jd.live.agent.governance.policy.variable.VariableParser;
import com.jd.live.agent.governance.policy.variable.VariableSource.ExpressionVariableSource;
import com.jd.live.agent.governance.request.RpcRequest;

import java.util.HashMap;
import java.util.Map;

@Injectable
@Extension(VariableParser.TYPE_EXPRESSION)
public class ExpressionVariableParser implements VariableParser<RpcRequest, ExpressionVariableSource> {

    private static final String KEY_ARGS = "args";

    @Inject
    private ExpressionEngine engine;

    @Override
    public String parse(RpcRequest request, ExpressionVariableSource source) {
        String result = null;
        String express = source == null ? null : source.getExpression();
        if (request != null && express != null && !express.isEmpty()) {
            Map<String, Object> context = new HashMap<>();
            context.put(KEY_ARGS, request.getArguments());
            Expression expression = engine.build(express);
            Object value = expression.evaluate(context);
            result = value == null ? null : value.toString();
        }
        return result;
    }
}
