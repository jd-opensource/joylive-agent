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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * CollectionOneToOneMatcher
 *
 * @param <T> Match target type
 * @since 1.0.0
 */
public class CollectionOneToOneMatcher<T> extends AbstractJunction<Iterable<? extends T>> {

    private final List<? extends ElementMatcher<? super T>> matchers;

    public CollectionOneToOneMatcher(List<? extends ElementMatcher<? super T>> matchers) {
        this.matchers = matchers;
    }

    @Override
    public boolean match(Iterable<? extends T> target) {
        if ((target instanceof Collection) && ((Collection<?>) target).size() != matchers.size()) {
            return false;
        }
        int count = 0;
        Iterator<? extends ElementMatcher<? super T>> iterator = matchers.iterator();
        for (T value : target) {
            if (!iterator.hasNext() || !iterator.next().match(value)) {
                return false;
            }
            count++;
        }
        return count == matchers.size();
    }
}
