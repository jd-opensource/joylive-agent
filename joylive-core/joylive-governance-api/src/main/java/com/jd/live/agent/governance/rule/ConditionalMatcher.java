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
package com.jd.live.agent.governance.rule;

import com.jd.live.agent.core.util.matcher.Matcher;

import java.util.List;

/**
 * The ConditionalMatcher interface represents an entity that can evaluate a set of conditions
 * against a given matcher and determine if a match exists based on a specified relation type.
 * This interface extends both Conditional and Matcher, allowing for the definition of conditions
 * and the matching logic to be encapsulated within the same interface.
 *
 * @param <T> the type of object being matched
 * @since 1.0.0
 */
public interface ConditionalMatcher<T> extends Conditional<T>, Matcher<Matcher<T>> {

    /**
     * Evaluates whether the given matcher matches any of the conditions specified in this conditional
     * matcher based on the relation type.
     *
     * @param matcher the matcher to evaluate against the conditions
     * @return true if the matcher matches the conditions based on the relation type; false otherwise
     */
    default boolean match(Matcher<T> matcher) {
        List<T> conditions = getConditions();
        if (matcher == null) {
            return false;
        } else if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        boolean matched = false;
        for (T condition : conditions) {
            if (matcher.match(condition)) {
                if (getRelationType() == RelationType.OR) {
                    return true;
                }
                matched = true;
            } else if (getRelationType() == RelationType.AND) {
                return false;
            }
        }
        return matched;
    }
}
