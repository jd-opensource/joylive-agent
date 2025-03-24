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

import java.util.Properties;

/**
 * Options represented as a Properties.
 */
public class PropertiesOption extends AbstractOption {

    protected Properties properties;

    public PropertiesOption(Properties properties) {
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getObject(final String key) {
        return properties == null ? null : (T) properties.get(key);
    }

    /**
     * Factory method to create a PropertiesOption from a given properties.
     *
     * @param properties A properties containing option keys and values.
     * @return An Option instance backed by the given properties.
     */
    public static Option of(Properties properties) {
        return new PropertiesOption(properties);
    }

}
