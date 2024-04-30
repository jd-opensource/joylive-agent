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
 * CollectionItemMatcher
 *
 * @since 1.0.0
 */
public class CollectionItemMatcher<T> extends AbstractJunction<Iterable<? extends T>> {

    private final ElementMatcher<? super T> matcher;

    public CollectionItemMatcher(ElementMatcher<? super T> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean match(Iterable<? extends T> target) {
        for (T value : target) {
            if (matcher.match(value)) {
                return true;
            }
        }
        return false;
    }
}
