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

import java.util.HashMap;
import java.util.Map;

/**
 * Options represented as a Map.
 */
public class MapOption extends AbstractOption {
    /**
     * The map containing options.
     */
    protected Map<String, ?> map;

    /**
     * Constructs a new MapOption with the specified map.
     *
     * @param map A map containing option keys and values.
     */
    public MapOption(Map<String, ?> map) {
        this.map = map;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getObject(final String key) {
        return map == null ? null : (T) map.get(key);
    }

    /**
     * Factory method to create a MapOption from a given map.
     *
     * @param map A map containing option keys and values.
     * @return An Option instance backed by the given map.
     */
    public static Option of(Map<String, ?> map) {
        return new MapOption(map);
    }

    /**
     * Factory method to create a MapOption from the system environment.
     *
     * @return An Option instance backed by the system properties and environment variables.
     */
    public static Option environment() {
        Map<String, Object> result = new HashMap<>();
        System.getProperties().forEach((key, value) -> result.put(key.toString(), value.toString()));
        result.putAll(System.getenv());
        return new MapOption(result);
    }

}
