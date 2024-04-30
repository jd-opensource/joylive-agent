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
package com.jd.live.agent.bootstrap.util.option;

import java.util.Map;
import java.util.function.Function;

/**
 * A class that implements both ValueSupplier and Function interfaces to resolve values from
 * configuration and environment variables, with support for parsing variable expressions.
 */
public class ConfigResolver implements ValueSupplier, Function<String, Object> {

    // A map containing configuration key-value pairs.
    private final Map<String, Object> config;

    // A map containing environment variable key-value pairs.
    private final Map<String, Object> env;

    // The ValueResolver used to parse and resolve variable expressions within the values.
    private final ValueResolver resolver = new ValueResolver(new ValueSupplier() {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getObject(String key) {
            return (T) env.get(key);
        }
    });

    /**
     * Constructs a new ConfigResolver with specified configuration and environment maps.
     *
     * @param config A map containing configuration key-value pairs.
     * @param env    A map containing environment variable key-value pairs.
     */
    public ConfigResolver(Map<String, Object> config, Map<String, Object> env) {
        this.config = config;
        this.env = env;
    }

    /**
     * Retrieves a configuration object based on the specified key, with support for parsing variable expressions.
     *
     * @param key The key to retrieve the value for.
     * @param <T> The type of the object to be returned.
     * @return The configuration object associated with the specified key, possibly resolved from a variable expression.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getObject(String key) {
        Object value = config.get(key);
        if (value instanceof String) {
            // Parse and resolve variable expressions within the value.
            value = resolver.parse((String) value);
        }
        return (T) value;
    }

    /**
     * Applies this function to the given argument, retrieving a configuration object based on the specified key.
     *
     * @param key The key to retrieve the value for.
     * @return The configuration object associated with the specified key.
     */
    @Override
    public Object apply(String key) {
        return getObject(key);
    }
}

