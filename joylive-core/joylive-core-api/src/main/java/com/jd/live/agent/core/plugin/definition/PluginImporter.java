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

import java.util.Map;

/**
 * This interface defines the contract for plugin importers that need to specify
 * a list of internal classes they require. Implementations of this interface
 * should return the fully qualified names of internal classes that are not part
 * of the module's exported or open packages but are necessary for the plugin's
 * operation.
 */
public interface PluginImporter {

    /**
     * Retrieves an array of fully qualified names of internal classes that the
     * plugin needs to import for its operations. These classes are considered
     * internal implementation details and are not part of the public API exposed
     * by the module.
     *
     * @return An array of strings representing the fully qualified names of
     * internal classes required by the plugin.
     */
    default String[] getImports() {
        return null;
    }

    /**
     * Retrieves a map of fully qualified names of internal classes to their corresponding import statements
     * that the plugin needs to import for its operations. These classes are considered internal implementation
     * details and are not part of the public API exposed by the module.
     *
     * @return A map where the keys are the fully qualified names of internal classes and the values are their
     * corresponding import statements required by the plugin. If no imports are needed, returns null.
     */
    default Map<String, String> getExports() {
        return null;
    }
}

