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
package com.jd.live.agent.core.util.option;

import java.util.Map;

/**
 * Options represented as a Map.
 */
public class MapOption<V> extends AbstractOption {
    /**
     * The map containing options.
     */
    protected Map<String, V> parameters;

    /**
     * Constructs a new MapOption with the specified map.
     *
     * @param parameters A map containing option keys and values.
     */
    public MapOption(Map<String, V> parameters) {
        this.parameters = parameters;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getObject(final String key) {
        return parameters == null ? null : (T) parameters.get(key);
    }

    /**
     * Factory method to create a MapOption from a given map.
     *
     * @param map A map containing option keys and values.
     * @return An Option instance backed by the given map.
     */
    public static <V> Option of(Map<String, V> map) {
        return new MapOption(map);
    }

}
