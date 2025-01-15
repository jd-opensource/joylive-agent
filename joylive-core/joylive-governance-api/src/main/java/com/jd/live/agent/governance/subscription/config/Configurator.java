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
package com.jd.live.agent.governance.subscription.config;

/**
 * An interface for retrieving configuration properties.
 *
 * @since 1.6.0
 */
public interface Configurator {

    /**
     * Returns the name of the configurator.
     *
     * @return The name of the configurator.
     */
    String getName();

    /**
     * Subscribes to the configurator for receiving configuration updates.
     *
     * @throws Exception If an error occurs during subscription.
     */
    void subscribe() throws Exception;

    /**
     * Retrieves the value of a configuration property from the application-specific source.
     *
     * @param name The name of the property to retrieve.
     * @return The value of the specified property, or null if the property does not exist.
     */
    Object getProperty(String name);

    /**
     * Adds a listener to receive notifications when configuration properties change.
     *
     * @param listener The listener to add.
     */
    void addListener(String name, ConfigListener listener);

    /**
     * Removes a listener that was previously added to receive notifications when configuration properties change.
     *
     * @param listener The listener to remove.
     */
    void removeListener(String name, ConfigListener listener);

}


