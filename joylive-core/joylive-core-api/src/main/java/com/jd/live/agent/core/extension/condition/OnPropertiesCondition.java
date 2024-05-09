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

import com.jd.live.agent.core.extension.annotation.ConditionalOnProperties;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.ConditionalRelation;

/**
 * Evaluates conditions based on a set of properties specified through the {@link ConditionalOnProperties} annotation.
 * This class allows for complex conditional logic to be applied based on the application's configuration or environment
 * properties, supporting both AND and OR logical relations among multiple {@link ConditionalOnProperty} conditions.
 * <p>
 * The decision to activate or deactivate a particular component or configuration can be made dynamically at runtime
 * based on the presence, absence, or specific values of certain properties. This facilitates flexible and adaptable
 * application behavior that can be customized through external configuration.
 * </p>
 */
public class OnPropertiesCondition extends OnCondition {

    /**
     * Evaluates the condition based on the {@link ConditionalOnProperties} annotation present in the given {@link ConditionContext}.
     * This method determines the logical relation (AND/OR) specified in the annotation and applies it to evaluate all
     * {@link ConditionalOnProperty} conditions defined.
     *
     * @param context The condition context providing access to the environment, annotated elements, and other conditions.
     * @return {@code true} if the conditions specified by {@link ConditionalOnProperties} are met according to the logical
     * relation defined; {@code false} otherwise.
     */
    @Override
    public boolean match(final ConditionContext context) {
        ConditionalOnProperties onProperties = (ConditionalOnProperties) context.getAnnotation();
        ConditionalRelation relation = onProperties.relation();
        for (ConditionalOnProperty onProperty : onProperties.value()) {
            boolean matched = OnPropertyCondition.CONDITION.match(context.create(onProperty));
            if (relation == ConditionalRelation.AND && !matched) {
                return false;
            } else if (relation == ConditionalRelation.OR && matched) {
                return true;
            }
        }
        return relation != ConditionalRelation.OR;
    }
}

