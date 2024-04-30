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

import java.util.List;

/**
 * The Conditional interface defines a contract for classes that represent a conditional
 * check or filter based on a set of conditions. Each condition is represented by an object
 * of type T, and the relation between these conditions is defined by the {@link RelationType}.
 *
 * @param <T> The type of conditions that this conditional holds.
 * @since 1.0.0
 */
public interface Conditional<T> {

    /**
     * Returns the relation type that defines how the conditions should be combined.
     * For example, if conditions are combined using an "AND" relation, all conditions must be
     * met for the conditional to be satisfied. If they are combined using an "OR" relation,
     * at least one condition must be met.
     *
     * @return The relation type between the conditions.
     */
    RelationType getRelationType();

    /**
     * Returns a list of conditions that this conditional object represents.
     * These conditions are evaluated based on the relation type specified by
     * {@link #getRelationType()}.
     *
     * @return A list of conditions.
     */
    List<T> getConditions();

}
