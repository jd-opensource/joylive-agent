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

import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClasses;

/**
 * The condition for the existence of some classes
 */
public class OnClassesCondition extends OnCondition {

    @Override
    public boolean match(final ConditionContext context) {
        ConditionalOnClasses onClasses = (ConditionalOnClasses) context.getAnnotation();
        for (ConditionalOnClass onClass : onClasses.value()) {
            if (!OnClassCondition.CONDITION.match(context.create(onClass))) {
                return false;
            }
        }
        return true;
    }

}
