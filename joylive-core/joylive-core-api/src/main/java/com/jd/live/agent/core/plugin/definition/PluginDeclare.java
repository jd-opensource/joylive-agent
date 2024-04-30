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
package com.jd.live.agent.core.plugin.definition;

import com.jd.live.agent.bootstrap.plugin.PluginPublisher;

import java.util.List;

/**
 * Declares the structure and behavior of a plugin within a plugin-based architecture.
 * This interface extends {@link PluginPublisher} to include additional methods for
 * plugin identification, definition retrieval, and class loader access.
 */
public interface PluginDeclare extends PluginPublisher {

    /**
     * Retrieves the name of the plugin.
     *
     * @return A {@link String} representing the name of the plugin.
     */
    String getName();

    /**
     * Retrieves a list of {@link PluginDefinition} instances associated with the plugin.
     * These definitions describe the specific functionalities or components provided by the plugin.
     *
     * @return A list of {@link PluginDefinition} instances, or an empty list if the plugin does not provide any.
     */
    List<PluginDefinition> getDefinitions();

    /**
     * Determines whether the plugin does not contain any definitions.
     *
     * @return {@code true} if the plugin has no definitions, otherwise {@code false}.
     */
    default boolean isEmpty() {
        List<PluginDefinition> definitions = getDefinitions();
        return definitions == null || definitions.isEmpty();
    }

    /**
     * Retrieves the class loader associated with the plugin. This class loader is used to load the plugin's
     * classes and resources.
     *
     * @return The {@link ClassLoader} associated with the plugin.
     */
    ClassLoader getClassLoader();
}
