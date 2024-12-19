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

import lombok.Getter;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * condition matcher
 */
public interface Condition {

    /**
     * match condition
     *
     * @param context ConditionContext
     * @return match result
     */
    boolean match(ConditionContext context);

    /**
     * A condition that delegates the condition matching to a provided condition.
     *
     * @see Condition
     * @see ConditionContext
     */
    @Getter
    class DelegateCondition implements Condition {

        private final Annotation annotation;

        private final Condition condition;

        public DelegateCondition(Annotation annotation, Condition condition) {
            this.annotation = annotation;
            this.condition = condition;
        }

        @Override
        public boolean match(ConditionContext context) {
            return condition.match(context.getAnnotation() == annotation ? context : context.create(annotation));
        }
    }

    /**
     * A composite condition that checks if all of its sub-conditions are met.
     * <p>
     * This class represents a composite condition that consists of multiple sub-conditions. The condition is considered
     * satisfied if and only if all of its sub-conditions are satisfied.
     *
     * @see Condition
     * @see ConditionContext
     */
    class CompositeCondition implements Condition {

        private final List<? extends Condition> conditions;

        public CompositeCondition(List<? extends Condition> conditions) {
            this.conditions = conditions;
        }

        @Override
        public boolean match(ConditionContext context) {
            for (Condition condition : conditions) {
                if (!condition.match(context)) {
                    return false;
                }
            }
            return true;
        }
    }

}