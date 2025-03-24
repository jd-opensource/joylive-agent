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
 * Represents system environment and properties as a configuration option.
 * This class extends {@link MapOption} to provide a unified view of both
 * system properties and environment variables as key-value pairs.
 */
public class EnvironmentOption extends MapOption {

    public EnvironmentOption() {
        super(env());
    }

    /**
     * Creates a map containing system properties and environment variables.
     * System properties are added first, followed by environment variables.
     * If there are duplicate keys, environment variables will override system properties.
     */
    private static Map<String, Object> env() {
        Map<String, Object> result = new HashMap<>();
        System.getProperties().forEach((key, value) -> result.put(key.toString(), value.toString()));
        result.putAll(System.getenv());
        return result;
    }
}
