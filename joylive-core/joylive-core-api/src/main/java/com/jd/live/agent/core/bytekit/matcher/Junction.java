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
 * Represents a combinator for {@link ElementMatcher} instances, allowing for logical operations
 * such as conjunction (AND) and disjunction (OR) between matchers. This interface extends
 * {@code ElementMatcher} and provides additional methods for combining matchers in a fluent
 * and expressive manner.
 * <p>
 * The {@code Junction} interface is useful when complex matching conditions need to be
 * composed from simpler ones. It enables the creation of compound matchers that can
 * evaluate to true only if all (AND) or any (OR) of their component matchers evaluate to true.
 * <p>
 * Implementations of this interface must provide the logic for the {@code and} and {@code or}
 * methods to combine matchers according to the specified logical operations.
 *
 * @param <T> the type of element to be matched by this junction
 * @since 2024-01-21
 */
public interface Junction<T> extends ElementMatcher<T> {

    /**
     * Creates a new junction that represents the logical AND of this matcher with another matcher.
     * The resulting junction will only match if both this matcher and the other matcher return true.
     *
     * @param other the other matcher to be combined with this one using logical AND
     * @param <U>   the type of element to be matched by the new junction, which is a subtype of T
     * @return a new junction representing the logical AND of this matcher with the other matcher
     */
    <U extends T> Junction<U> and(ElementMatcher<? super U> other);

    /**
     * Creates a new junction that represents the logical OR of this matcher with another matcher.
     * The resulting junction will match if either this matcher or the other matcher returns true.
     *
     * @param other the other matcher to be combined with this one using logical OR
     * @param <U>   the type of element to be matched by the new junction, which is a subtype of T
     * @return a new junction representing the logical OR of this matcher with the other matcher
     */
    <U extends T> Junction<U> or(ElementMatcher<? super U> other);
}

