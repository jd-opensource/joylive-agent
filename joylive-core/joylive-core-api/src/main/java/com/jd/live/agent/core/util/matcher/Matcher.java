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
package com.jd.live.agent.core.util.matcher;

/**
 * Represents a generic matching functionality, allowing for the implementation of custom match logic.
 * This functional interface defines a single method {@code match}, which determines if a given object
 * matches certain criteria.
 *
 * @param <T> The type of object that this matcher will evaluate.
 */
@FunctionalInterface
public interface Matcher<T> {

    /**
     * Evaluates the given object against this matcher's criteria.
     *
     * @param source The object to be evaluated.
     * @return {@code true} if the object matches the criteria, {@code false} otherwise.
     */
    boolean match(T source);

}
