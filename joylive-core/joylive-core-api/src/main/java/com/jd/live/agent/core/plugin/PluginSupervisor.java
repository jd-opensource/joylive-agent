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
package com.jd.live.agent.core.plugin;

import java.util.Collections;
import java.util.Set;

/**
 * An interface defining the contract for managing the lifecycle of plugins,
 * including their installation and uninstallation.
 */
public interface PluginSupervisor extends AutoCloseable {

    /**
     * Installs plugins based on the specified installation mode.
     *
     * @param dynamic Specifies whether the installation should be dynamic.
     * @return {@code true} if the installation was successful; {@code false} otherwise.
     */
    boolean install(boolean dynamic);

    /**
     * Attempts to install a plugin identified by its name.
     *
     * @param name The name of the plugin to install.
     * @return {@code true} if the installation was successful; {@code false} otherwise.
     */
    default boolean install(String name) {
        return name == null || name.isEmpty() || install(Collections.singleton(name));
    }

    /**
     * Installs a set of plugins identified by their names.
     *
     * @param names The names of the plugins to install.
     * @return {@code true} if all specified plugins were successfully installed; {@code false} otherwise.
     */
    boolean install(Set<String> names);

    /**
     * Uninstalls all currently installed plugins.
     */
    void uninstall();

    /**
     * Attempts to uninstall a plugin identified by its name.
     *
     * @param name The name of the plugin to uninstall.
     */
    default void uninstall(String name) {
        if (name != null && !name.isEmpty()) {
            uninstall(Collections.singleton(name));
        }
    }

    /**
     * Uninstalls a set of plugins identified by their names.
     *
     * @param names The names of the plugins to uninstall.
     */
    void uninstall(Set<String> names);

    @Override
    default void close() {
        uninstall();
    }
}
