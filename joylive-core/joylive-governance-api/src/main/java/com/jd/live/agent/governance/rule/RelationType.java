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

/**
 * The RelationType enum defines the types of relations that can be used to combine multiple
 * conditions within a {@link Conditional}. The available relation types are "AND" and "OR",
 * which dictate how the conditions are logically combined to determine the outcome.
 */
public enum RelationType {

    /**
     * The AND relation type. Indicates that all conditions must be satisfied for the
     * {@link Conditional} object to be considered as meeting the criteria.
     */
    AND,

    /**
     * The OR relation type. Indicates that at least one condition must be satisfied for the
     * {@link Conditional} object to be considered as meeting the criteria.
     */
    OR;

}
