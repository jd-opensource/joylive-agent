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
package com.jd.live.agent.core.bootstrap;

import java.util.Map;

/**
 * An interface for retrieving configuration properties.
 *
 * @since 1.6.0
 */
public interface AppPropertySource {

    /**
     * Retrieves the value of a configuration property from the application-specific source.
     *
     * @param name The name of the property to retrieve.
     * @return The value of the specified property, or null if the property does not exist.
     */
    String getProperty(String name);

    /**
     * Returns the name of the object.
     *
     * @return The name of the object.
     */
    String getName();

    /**
     * A property source implementation backed by a Map.
     */
    class MapSource implements AppPropertySource {

        private final String name;

        private final Map<String, String> map;

        public MapSource(String name, Map<String, String> map) {
            this.name = name;
            this.map = map;
        }

        @Override
        public String getProperty(String name) {
            return name == null || map == null ? null : map.get(name);
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
