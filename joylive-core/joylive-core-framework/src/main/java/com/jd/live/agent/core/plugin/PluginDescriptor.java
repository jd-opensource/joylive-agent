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

import com.jd.live.agent.core.plugin.definition.PluginDeclare;

import java.util.function.Consumer;

/**
 * An interface representing a plugin descriptor. It extends the {@link PluginDeclare} interface
 * and provides methods to manage the status and lifecycle of a plugin, including loading, uninstalling,
 * and handling success or failure scenarios.
 */
public interface PluginDescriptor extends PluginDeclare {

    /**
     * Retrieves the current status of the plugin.
     *
     * @return The {@link PluginStatus} of the plugin.
     */
    PluginStatus getStatus();

    /**
     * Retrieves the type of the plugin.
     *
     * @return The {@link PluginType} of the plugin.
     */
    PluginType getType();

    /**
     * Initiates the loading of plugin definitions. If the plugin is in the CREATED status, it attempts
     * to load the definitions and transitions to the LOADED status.
     */
    boolean load();

    /**
     * Uninstalls the plugin. This method should handle any necessary cleanup or removal of the plugin.
     */
    void uninstall();

    /**
     * Marks the plugin as successfully loaded and publishes a success event to all listeners.
     */
    void success();

    /**
     * Marks the plugin as failed to load, updates its status, and publishes a failure event to all listeners.
     *
     * @param message   A message describing the failure.
     * @param throwable The exception that caused the failure.
     */
    void fail(String message, Throwable throwable);

    /**
     * Releases resources associated with the plugin.
     * If a recycler is provided, it is used to release resources related to the class loader.
     *
     * @param recycler a consumer that can be used to release resources related to the class loader (can be null)
     */
    void release(Consumer<ClassLoader> recycler);
}

