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

import com.jd.live.agent.bootstrap.classloader.ClassLoaderSupervisor;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.plugin.PluginEvent;
import com.jd.live.agent.bootstrap.plugin.PluginEvent.EventType;
import com.jd.live.agent.bootstrap.plugin.PluginListener;
import com.jd.live.agent.core.bytekit.ByteBuilder;
import com.jd.live.agent.core.bytekit.ByteSupplier;
import com.jd.live.agent.core.bytekit.transformer.Resetter;
import com.jd.live.agent.core.config.AgentPath;
import com.jd.live.agent.core.config.PluginConfig;
import com.jd.live.agent.core.extension.ExtensionManager;
import com.jd.live.agent.core.plugin.Plugin.PluginType;
import com.jd.live.agent.core.plugin.Plugin.Status;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.util.Close;
import lombok.Getter;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages the lifecycle of plugins within an agent context. This includes installing, enhancing,
 * and uninstalling both static and dynamic plugins.
 *
 * @since 1.0.0
 */
public class PluginManager implements PluginSupervisor {
    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);

    private final Instrumentation instrumentation;

    private final PluginConfig pluginConfig;

    private final AgentPath agentPath;

    /**
     * The extension manager responsible for managing extensions.
     */
    private final ExtensionManager extensionManager;

    /**
     * The class loader supervisor responsible for supervising class loaders.
     */
    private final ClassLoaderSupervisor supervisor;

    /**
     * The byte supplier that provides byte data.
     */
    private final ByteSupplier byteSupplier;

    /**
     * A map storing installed plugins.
     */
    private final Map<String, InstallPlugin> installed = new ConcurrentHashMap<>();

    /**
     * Listens to plugin events and handles them accordingly by logging and managing plugin states.
     */
    private final PluginListener pluginListener = event -> {
        switch (event.getType()) {
            case SUCCESS:
                onSuccess(event);
                break;
            case FAIL:
                onFail(event);
                break;
            case UNINSTALL:
                onUninstall(event);
        }
    };

    public PluginManager(Instrumentation instrumentation,
                         PluginConfig pluginConfig,
                         AgentPath agentPath,
                         ExtensionManager extensionManager,
                         ClassLoaderSupervisor supervisor,
                         ByteSupplier byteSupplier) {

        this.instrumentation = instrumentation;
        this.pluginConfig = pluginConfig == null ? new PluginConfig() : pluginConfig;
        this.agentPath = agentPath;
        this.extensionManager = extensionManager;
        this.supervisor = supervisor;
        this.byteSupplier = byteSupplier;
    }

    @Override
    public synchronized boolean install(boolean dynamic) {
        InstallMode mode = dynamic ? InstallMode.DYNAMIC : InstallMode.STATIC;
        // system plugin install first
        List<Plugin> statics = install(pluginConfig.getSystems(), mode);
        // Automatically install any static plugins that are not installed.
        statics.addAll(install(pluginConfig.getStatics(), mode));
        if (enhanceStatic(statics, mode)) {
            if (dynamic) {
                return enhanceDynamic(install(pluginConfig.getDynamics(), mode));
            }
            return true;
        } else {
            return false;
        }

    }

    @Override
    public synchronized boolean install(Set<String> names) {
        return enhanceDynamic(install(names, InstallMode.DYNAMIC));
    }

    /**
     * Installs plugins based on the given set of plugin names and the dynamic flag.
     *
     * @param names   the set of plugin names to install
     * @param mode    the installing mode
     * @return a list of plugins to install
     */
    private List<Plugin> install(Set<String> names, InstallMode mode) {
        List<Plugin> result = new LinkedList<>();
        if (names != null) {
            Map<String, File> files = agentPath.getPlugins();
            for (String name : names) {
                File file = files.get(name);
                if (file != null && pluginConfig.isActive(name, mode == InstallMode.DYNAMIC)) {
                    InstallPlugin plugin = installed.computeIfAbsent(name, n -> new InstallPlugin(createPlugin(file), mode));
                    switch (plugin.getStatus()) {
                        case CREATED:
                        case FAILED:
                        case LOADED:
                            result.add(plugin.plugin);
                            break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Enhances the given list of plugins dynamically.
     *
     * @param plugins the list of plugins to enhance
     * @return true if the enhancement is successful, false otherwise
     */
    private boolean enhanceDynamic(List<Plugin> plugins) {
        if (plugins == null || plugins.isEmpty()) {
            return true;
        }
        // a transaction
        boolean success = loadPlugins(plugins);
        List<Plugin> enhances = plugins;
        if (success) {
            enhances = plugins.stream().filter(p -> p.getStatus() == Status.LOADED && !p.isEmpty()).collect(Collectors.toList());
            for (Plugin plugin : enhances) {
                try {
                    Resetter resetter = byteSupplier.create().append(plugin).install(instrumentation);
                    plugin.addListener(p -> {
                        if (p.getType() == EventType.UNINSTALL) {
                            resetter.reset();
                        }
                    });
                } catch (Throwable e) {
                    plugin.fail("Failed to enhance plugin " + plugin.getName(), e);
                    success = false;
                    break;
                }
            }
        }
        if (!success) {
            logger.info("Start uninstalling plugins when an exception occurs");
            enhances.forEach(Plugin::uninstall);
        } else {
            enhances.forEach(Plugin::success);
        }
        return success;
    }

    /**
     * Enhances static plugins by loading and installing them if they meet certain conditions.
     *
     * @param plugins the list of plugins to be enhanced.
     * @param mode the installing mode.
     * @return true if the enhancement process was successful, false otherwise.
     */
    private boolean enhanceStatic(List<Plugin> plugins, InstallMode mode) {
        if (plugins == null || plugins.isEmpty()) {
            return true;
        }
        // a transaction
        boolean success = loadPlugins(plugins);
        List<Plugin> enhances = plugins;
        if (success) {
            enhances = plugins.stream().filter(p -> p.getStatus() == Status.LOADED && !p.isEmpty()).collect(Collectors.toList());
            if (!enhances.isEmpty()) {
                ByteBuilder builder = byteSupplier.create();
                for (Plugin plugin : enhances) {
                    builder = builder.append(plugin);
                }
                try {
                    Resetter resetter = builder.install(instrumentation);
                    if (mode == InstallMode.DYNAMIC) {
                        enhances.get(0).addListener(p -> {
                            if (p.getType() == EventType.UNINSTALL) {
                                resetter.reset();
                            }
                        });
                    }
                } catch (Throwable e) {
                    logger.error("Failed to enhance static plugins", e);
                    success = false;
                }
            }
        }
        if (!success) {
            logger.info("Start uninstalling plugins when an exception occurs");
            enhances.forEach(Plugin::uninstall);
        } else {
            enhances.forEach(Plugin::success);
        }
        return success;
    }

    /**
     * Loads a list of plugins by invoking their load method.
     * If any plugin fails to load, the process is halted and false is returned.
     *
     * @param plugins The list of plugins to be loaded.
     * @return true if all plugins are loaded successfully, false if any plugin fails to load.
     */
    private boolean loadPlugins(List<Plugin> plugins) {
        for (Plugin plugin : plugins) {
            plugin.load();
            if (plugin.getStatus() == Status.FAILED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a new Plugin instance from a given file.
     *
     * @param file    The file from which the plugin is created.
     * @return The created Plugin instance.
     */
    private Plugin createPlugin(File file) {
        String name = file.getName();
        URL[] urls = agentPath.getLibUrls(file);
        ClassLoader classLoader = supervisor.create(name, urls);
        Plugin plugin = new Plugin(file, getPluginType(name), urls, extensionManager.build(PluginDefinition.class, classLoader));
        plugin.addListener(pluginListener);
        return plugin;
    }

    /**
     * Determines the {@link PluginType} based on the plugin name.
     *
     * @param name the name of the plugin
     * @return the {@link PluginType} corresponding to the plugin name, or {@code null} if the name is {@code null}
     * or does not match any known plugin type
     */
    private PluginType getPluginType(String name) {
        if (name == null) {
            return null;
        } else if (pluginConfig.isSystem(name)) {
            return PluginType.SYSTEM;
        } else if (pluginConfig.isStatic(name)) {
            return PluginType.STATIC;
        } else if (pluginConfig.isDynamic(name)) {
            return PluginType.DYNAMIC;
        }
        return null;
    }

    /**
     * Handles the uninstallation of a plugin from the system. This process includes:
     * - Removing the plugin from the class loader supervisor.
     * - Removing the plugin from the extension manager.
     * - Closing the plugin's class loader if it implements {@link AutoCloseable}.
     *
     * @param event The plugin event containing details about the plugin to be uninstalled.
     */
    private void onUninstall(PluginEvent event) {
        logger.info(event.getMessage());
        Plugin plugin = event.getOwner();
        remove(plugin);
        ClassLoader loader = plugin.getClassLoader();
        supervisor.remove(plugin.getName());
        extensionManager.remove(loader);
        Close.instance().close(loader instanceof AutoCloseable ? (AutoCloseable) loader : null);
    }

    /**
     * Handles the successful completion of a plugin event.
     *
     * @param event The plugin event that completed successfully.
     */
    private void onSuccess(PluginEvent event) {
        logger.info(event.getMessage());
    }

    /**
     * Handles the failure of a plugin event. This process includes:
     * - Logging the failure message and throwable.
     * - Removing the plugin from the system.
     *
     * @param event The plugin event that failed.
     */
    private void onFail(PluginEvent event) {
        logger.info(event.getMessage(), event.getThrowable());
        remove(event.getOwner());
    }

    /**
     * Removes a plugin from the cache.
     *
     * @param plugin The plugin to be removed.
     */
    private void remove(Plugin plugin) {
        installed.remove(plugin.getName());
    }

    @Override
    public synchronized void uninstall() {
        for (InstallPlugin plugin : installed.values()) {
            if (plugin.getMode() == InstallMode.DYNAMIC) {
                remove(plugin.plugin);
                plugin.uninstall();
            }
        }
    }

    @Override
    public synchronized void uninstall(Set<String> names) {
        if (names != null) {
            for (String name : names) {
                if (name != null && !name.isEmpty()) {
                    InstallPlugin plugin = installed.get(name);
                    // Only dynamic plugins with the specified name can be deleted
                    if (plugin != null && plugin.getType() == PluginType.DYNAMIC) {
                        installed.remove(name);
                        plugin.uninstall();
                    }
                }
            }
        }
    }

    private static class InstallPlugin {

        private final Plugin plugin;

        @Getter
        private final InstallMode mode;

        InstallPlugin(Plugin plugin, InstallMode mode) {
            this.plugin = plugin;
            this.mode = mode;
        }

        public String getName() {
            return plugin.getName();
        }

        public PluginType getType() {
            return plugin.getType();
        }

        public void uninstall() {
            plugin.uninstall();
        }

        public Status getStatus() {
            return plugin.getStatus();
        }

        public boolean isEmpty() {
            return plugin.isEmpty();
        }
    }

    /**
     * The {@code InstallMode} enum represents the installation mode of a plugin.
     */
    private enum InstallMode {
        /**
         * Static installation mode.
         */
        STATIC,

        /**
         * Dynamic installation mode.
         */
        DYNAMIC
    }

}
