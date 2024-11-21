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

import com.jd.live.agent.bootstrap.plugin.PluginEvent;
import com.jd.live.agent.bootstrap.plugin.PluginEvent.EventType;
import com.jd.live.agent.bootstrap.plugin.PluginListener;
import com.jd.live.agent.core.extension.ExtensibleLoader;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.plugin.definition.PluginDeclare;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import lombok.Getter;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a plugin with capabilities to load, manage, and interact with plugin definitions.
 * A plugin is identified by its name and path, and can be dynamic or static. It maintains a lifecycle
 * status and notifies listeners about relevant events.
 *
 */
public class Plugin implements PluginDeclare {

    /**
     * The name of the plugin.
     */
    @Getter
    private final String name;

    /**
     * The file path of the plugin's location.
     */
    @Getter
    private final File path;

    /**
     * Indicates whether the plugin is dynamic.
     */
    @Getter
    private final PluginType type;

    /**
     * An array of {@link URL} objects that the plugin uses for class loading.
     */
    @Getter
    private final URL[] urls;

    /**
     * The class loader used to load plugin definitions.
     */
    private final ExtensibleLoader<PluginDefinition> loader;

    /**
     * A thread-safe list of plugin listeners to notify about plugin events.
     */
    private final List<PluginListener> listeners = new CopyOnWriteArrayList<>();

    @Getter
    private Status status = Status.CREATED;

    @Getter
    private List<PluginDefinition> definitions;

    /**
     * Constructs a new Plugin with specified parameters.
     *
     * @param path     The file path of the plugin's location.
     * @param name     The name of the plugin.
     * @param type     The type of the plugin.
     * @param urls     An array of {@link URL} objects for class loading.
     * @param loader   The class loader to use for loading plugin definitions.
     */
    public Plugin(File path, String name, PluginType type, URL[] urls, ExtensibleLoader<PluginDefinition> loader) {
        this.path = path;
        this.name = name;
        this.type = type;
        this.urls = urls;
        this.loader = loader;
    }

    /**
     * Constructs a new Plugin with a path, indicating whether it's dynamic, URLs for class loading,
     * and a loader for plugin definitions. The plugin name is derived from the file name of the path.
     *
     * @param path     The file path of the plugin's location.
     * @param type     The type of the plugin.
     * @param urls     An array of {@link URL} objects for class loading.
     * @param loader   The class loader to use for loading plugin definitions.
     */
    public Plugin(File path, PluginType type, URL[] urls, ExtensibleLoader<PluginDefinition> loader) {
        this(path, path.getName(), type, urls, loader);
    }

    /**
     * Constructs a new Plugin with a path, name, and a loader for plugin definitions. The plugin is
     * considered not dynamic and does not use URLs for class loading.
     *
     * @param path   The file path of the plugin's location.
     * @param name   The name of the plugin.
     * @param loader The class loader to use for loading plugin definitions.
     */
    public Plugin(File path, String name, ExtensibleLoader<PluginDefinition> loader) {
        this(path, name, PluginType.STATIC, null, loader);
    }

    public ClassLoader getClassLoader() {
        return loader == null ? null : loader.getClassLoader();
    }

    @Override
    public void addListener(PluginListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Initiates the loading of plugin definitions. If the plugin is in the CREATED status, it attempts
     * to load the definitions and transitions to the LOADED status.
     */
    public void load() {
        if (status == Status.CREATED) {
            try {
                definitions = loader.loadExtensible().getExtensions();
                status = Status.LOADED;
            } catch (Throwable e) {
                fail("failed to load plugin " + name, e);
            }
        }
    }

    /**
     * Marks the plugin as successfully loaded and publishes a success event to all listeners.
     */
    public void success() {
        if (status == Status.LOADED) {
            status = Status.SUCCESS;
            List<String> names = new ArrayList<>(definitions.size());
            definitions.forEach(t -> {
                Extension extension = t.getClass().getAnnotation(Extension.class);
                String[] values = extension.value();
                if (values != null && values.length > 0) {
                    names.addAll(Arrays.asList(values));
                } else {
                    names.add(t.getClass().getSimpleName());
                }
            });
            publish(new PluginEvent(this, EventType.SUCCESS, "Install plugin " + name + ". definitions=" + names));
        }
    }

    /**
     * Marks the plugin as failed to load, updates its status, and publishes a failure event to all listeners.
     *
     * @param message   A message describing the failure.
     * @param throwable The exception that caused the failure.
     */
    public void fail(String message, Throwable throwable) {
        status = Status.FAILED;
        publish(new PluginEvent(this, EventType.FAIL, message, throwable));
    }

    /**
     * Uninstalls the plugin by setting its status back to LOADED and publishes an uninstall event.
     */
    public void uninstall() {
        if (status == Status.SUCCESS) {
            status = Status.LOADED;
        } else if (status == Status.FAILED) {
            status = definitions == null ? Status.CREATED : Status.LOADED;
        }
        publish(new PluginEvent(this, EventType.UNINSTALL, "Uninstall plugin " + name));
    }

    /**
     * Publishes a given plugin event to all registered listeners.
     *
     * @param event The {@link PluginEvent} to publish.
     */
    public void publish(PluginEvent event) {
        if (event != null) {
            for (PluginListener listener : listeners) {
                try {
                    listener.onEvent(event);
                } catch (Throwable ignore) {
                }
            }
        }
    }

    /**
     * Represents the various statuses a plugin can be in during its lifecycle.
     */
    public enum Status {

        /**
         * The plugin has been created but not yet loaded
         */
        CREATED,

        /**
         * The plugin's definitions have been loaded
         */
        LOADED,

        /**
         * The plugin has been successfully loaded and is operational
         */
        SUCCESS,

        /**
         * The plugin has failed to load properly
         */
        FAILED
    }

    /**
     * The {@code PluginType} enum represents the type of a plugin.
     */
    public enum PluginType {
        /**
         * System plugin.
         */
        SYSTEM,

        /**
         * Static plugin.
         */
        STATIC,

        /**
         * Dynamic plugin.
         */
        DYNAMIC
    }
}
