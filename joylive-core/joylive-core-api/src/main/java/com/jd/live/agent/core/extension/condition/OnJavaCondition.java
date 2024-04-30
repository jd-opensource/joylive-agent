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
package com.jd.live.agent.core.extension.condition;

import com.jd.live.agent.core.extension.annotation.ConditionalOnJava;
import com.jd.live.agent.core.util.version.JVM;
import com.jd.live.agent.core.util.version.VersionExpression;

/**
 * Java version condition
 */
public class OnJavaCondition extends OnCondition {

    @Override
    public boolean match(final ConditionContext context) {
        ConditionalOnJava onJava = (ConditionalOnJava) context.getAnnotation();
        if (!isEmpty(onJava.value())) {
            VersionExpression expression = new VersionExpression(onJava.value());
            return expression.match(JVM.instance().getVersion());
        }
        return true;
    }

}
