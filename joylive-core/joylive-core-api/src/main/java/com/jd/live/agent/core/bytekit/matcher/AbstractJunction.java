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
 * Represents an abstract base class for junctions, which are logical constructs that can combine
 * multiple conditions or matchers using AND and OR operations.
 *
 * @param <T> The type of elements accepted by the junction.
 * @since 1.0.0
 */
public abstract class AbstractJunction<T> implements Junction<T> {

    @SuppressWarnings("unchecked")
    @Override
    public <U extends T> Junction<U> and(ElementMatcher<? super U> other) {
        return new Conjunction<>(this, other);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U extends T> Junction<U> or(ElementMatcher<? super U> other) {
        return new Disjunction<>(this, other);
    }
}
