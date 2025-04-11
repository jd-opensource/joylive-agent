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
package com.jd.live.agent.core.util.map;

import java.util.Map;
import java.util.function.Function;

@FunctionalInterface
public interface MapBuilder<K, T> {
    Map<K, T> build();

    default Function<K, K> getKeyConverter() {
        return null;
    }

    /**
     * A {@link MapBuilder} extension that converts all keys to lowercase before insertion.
     * This ensures case-insensitive key comparisons in the resulting map.
     *
     * @param <T> the type of values stored in the map
     *
     * @see MapBuilder
     */
    interface LowercaseMapBuilder<T> extends MapBuilder<String, T> {

        @Override
        default Function<String, String> getKeyConverter() {
            return String::toLowerCase;
        }
    }
}
