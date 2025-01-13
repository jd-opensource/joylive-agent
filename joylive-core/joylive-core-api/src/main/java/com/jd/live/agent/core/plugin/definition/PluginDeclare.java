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
import com.jd.live.agent.core.bytekit.type.TypeDesc;

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
     * Matches the plugin definitions with the given {@link TypeDesc}.
     *
     * @param typeDesc the type descriptor to match against the plugin definitions
     * @return A list of {@link PluginDefinition} instances that match the given type descriptor,
     * or an empty list if no matches are found.
     */
    List<PluginDefinition> match(TypeDesc typeDesc, ClassLoader classLoader);

    /**
     * Determines whether the plugin does not contain any definitions.
     *
     * @return {@code true} if the plugin has no definitions, otherwise {@code false}.
     */
    default boolean isEmpty() {
        List<PluginDefinition> definitions = getDefinitions();
        return definitions == null || definitions.isEmpty();
    }
}
