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

import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.ConditionalRelation;

/**
 * Condition implementation that checks if a specific property is set in the environment.
 */
public class OnPropertyCondition extends OnCondition {

    // Singleton instance of OnPropertyCondition
    public static final OnPropertyCondition CONDITION = new OnPropertyCondition();

    @Override
    public boolean match(final ConditionContext context) {
        ConditionalOnProperty onProperty = (ConditionalOnProperty) context.getAnnotation();
        String[] keys = onProperty.name();
        String value = onProperty.value();
        // If the property name is not provided
        if (keys.length == 0) {
            if (!isEmpty(value)) {
                return isMatched(context, onProperty, value, null);
            } else {
                // If neither name nor value is provided, the condition does not match
                return false;
            }
        } else {
            ConditionalRelation relation = onProperty.relation();
            for (String key : keys) {
                boolean matched = isMatched(context, onProperty, key, value);
                if (relation == ConditionalRelation.OR && matched) {
                    return true;
                } else if (relation == ConditionalRelation.AND && !matched) {
                    return false;
                }
            }
            return relation != ConditionalRelation.OR;
        }
    }

    private boolean isMatched(ConditionContext context, ConditionalOnProperty onProperty, String key, String value) {
        // If the property name is provided, retrieve its value from the context
        String config = context.geConfig(key);
        // If the property is missing, defer to 'matchIfMissing' attribute
        // If a value is provided, it must match the property value; otherwise, expect a boolean 'true'
        if (isEmpty(config)) {
            return onProperty.matchIfMissing();
        } else if (isEmpty(value)) {
            return Boolean.parseBoolean(config);
        } else if (onProperty.caseSensitive()) {
            return value.equals(config);
        } else {
            return value.equalsIgnoreCase(config);
        }
    }

}
