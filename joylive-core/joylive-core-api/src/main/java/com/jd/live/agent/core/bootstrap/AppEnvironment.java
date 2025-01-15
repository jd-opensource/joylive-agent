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

/**
 * An interface representing the application environment, which provides access to configuration properties.
 *
 * @since 1.6.0
 */
public interface AppEnvironment {

    /**
     * Adds a new ApplicationPropertySource to the beginning of the property source list.
     *
     * @param propertySource The property source to add.
     */
    void addFirst(AppPropertySource propertySource);

    /**
     * Adds a new ApplicationPropertySource to the end of the property source list.
     *
     * @param propertySource The property source to add.
     */
    void addLast(AppPropertySource propertySource);
}
