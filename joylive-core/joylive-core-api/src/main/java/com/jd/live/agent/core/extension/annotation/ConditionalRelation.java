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
package com.jd.live.agent.core.extension.annotation;

/**
 * Defines the types of logical relations that can be used to combine multiple conditions or criteria.
 */
public enum ConditionalRelation {

    /**
     * Indicates that all conditions combined with this relation type must be true
     * for the overall expression to be true. It is akin to a logical "AND" operation
     * in boolean logic, where {@code true AND true} is {@code true}, but any
     * {@code false} condition will make the overall result {@code false}.
     */
    AND,

    /**
     * Indicates that only one of the conditions combined with this relation type needs to be true
     * for the overall expression to be true. This is similar to a logical "OR" operation
     * in boolean logic, where {@code false OR true} is {@code true}, and only when all
     * conditions are {@code false}, the overall result is {@code false}.
     */
    OR

}
