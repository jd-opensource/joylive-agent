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

/**
 * Functional interface for building maps with optional key conversion.
 */
@FunctionalInterface
public interface MapBuilder<K, T> {

    /**
     * Builds and returns a new map.
     *
     * @return the built map
     */
    Map<K, T> build();

    /**
     * Returns key converter function for map keys.
     *
     * @return key converter or null if no conversion needed
     */
    default Function<K, K> getKeyConverter() {
        return null;
    }
}