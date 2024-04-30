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
package com.jd.live.agent.core.bytekit.matcher;

/**
 * Represents a functional interface for matching elements based on custom criteria.
 * This interface is designed to be implemented by classes that specify the logic
 * for matching elements of type {@code T}. The primary method {@code match} determines
 * whether a given element satisfies the matching criteria.
 * <p>
 * {@code ElementMatcher} is a key component in systems where elements (such as classes,
 * methods, fields, etc.) need to be filtered or selected based on specific conditions.
 * It provides a generic and flexible way to define these conditions.
 * <p>
 * Implementations of this interface are expected to provide a boolean result indicating
 * whether the provided element matches the defined criteria. This makes {@code ElementMatcher}
 * suitable for a wide range of matching scenarios.
 *
 * @param <T> the type of element to be matched
 * @since 2024-01-20
 */
@FunctionalInterface
public interface ElementMatcher<T> {

    /**
     * Determines whether the specified element matches the criteria defined by this matcher.
     *
     * @param target the element to be matched
     * @return {@code true} if the element matches the criteria; {@code false} otherwise
     */
    boolean match(T target);
}

