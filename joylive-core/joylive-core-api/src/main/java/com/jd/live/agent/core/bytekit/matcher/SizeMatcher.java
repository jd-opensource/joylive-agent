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

/**
 * SizeMatcher
 *
 * @param <T> type of iterable
 * @since 1.0.0
 */
public class SizeMatcher<T extends Iterable<?>> extends AbstractJunction<T> {

    private final int size;

    public SizeMatcher(int size) {
        this.size = size;
    }

    @Override
    public boolean match(T target) {
        if (target == null)
            return size == 0;
        else if (target instanceof Collection) {
            return ((Collection<?>) target).size() == size;
        } else {
            int size = 0;
            for (Object ignored : target) {
                size++;
            }
            return size == this.size;
        }
    }
}
