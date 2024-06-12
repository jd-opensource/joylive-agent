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
import com.jd.live.agent.core.config.PluginConfig;
import com.jd.live.agent.core.context.AgentContext;
import com.jd.live.agent.core.extension.ExtensionManager;
import com.jd.live.agent.core.plugin.Plugin.Status;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.util.Close;

import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages the lifecycle of plugins within an agent context. This includes installing, enhancing,
 * and uninstalling both static and dynamic plugins. It interacts with an {@link AgentContext},
 * {@link ExtensionManager}, and a {@link ClassLoaderSupervisor} to manage plugin resources and class loading.
 * Additionally, it uses a {@link ByteSupplier} to manipulate bytecode for plugin enhancement.
 *
 * @since 1.0.0
 */
public class PluginManager implements PluginSupervisor {
    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);

    /**
     * The context object containing agent configuration and paths.
     */
    private final AgentContext context;

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
     * A map storing static plugins, where the key is the plugin name and the value is the corresponding Plugin object.
     */
    private final Map<String, Plugin> statics = new ConcurrentHashMap<>();

    /**
     * A map storing dynamic plugins, where the key is the plugin name and the value is the corresponding Plugin object.
     */
    private final Map<String, Plugin> dynamics = new ConcurrentHashMap<>();

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

    /**
     * Constructs a new PluginManager with the specified context, extension manager, class loader supervisor,
     * and byte supplier.
     *
     * @param context           The agent context.
     * @param extensionManager  The manager for plugin extensions.
     * @param supervisor        The supervisor for class loaders.
     * @param byteSupplier      The supplier for byte manipulation utilities.
     */
    public PluginManager(AgentContext context, ExtensionManager extensionManager, ClassLoaderSupervisor supervisor,
                         ByteSupplier byteSupplier) {
        this.context = context;
        this.extensionManager = extensionManager;
        this.supervisor = supervisor;
        this.byteSupplier = byteSupplier;
    }

    @Override
    public synchronized boolean install(boolean dynamic) {
        PluginConfig pluginConfig = context.getAgentConfig().getPluginConfig();
        // system plugin install first
        List<Plugin> enhanceStatics = install(pluginConfig == null ? null : pluginConfig.getSystems(), false);
        // Automatically install any static plugins that are not installed.
        enhanceStatics.addAll(install(pluginConfig == null ? null : pluginConfig.getStatics(), false));
        if (enhanceStatic(enhanceStatics)) {
            if (dynamic) {
                return enhanceDynamic(install(pluginConfig == null ? null : pluginConfig.getDynamics(), true));
            }
            return true;
        } else {
            return false;
        }

    }

    @Override
    public synchronized boolean install(Set<String> names) {
        return enhanceDynamic(install(names, true));
    }

    /**
     * Installs plugins based on the given set of plugin names and the dynamic flag.
     *
     * @param names   the set of plugin names to install
     * @param dynamic a flag indicating whether the plugins are dynamic or static
     * @return a list of installed plugins
     */
    private List<Plugin> install(Set<String> names, boolean dynamic) {
        List<Plugin> installed = new LinkedList<>();
        if (names != null) {
            PluginConfig pluginConfig = context.getAgentConfig().getPluginConfig();
            Map<String, File> files = context.getAgentPath().getPlugins();
            Map<String, Plugin> plugins = dynamic ? dynamics : statics;
            for (String name : names) {
                File file = files.get(name);
                if (file != null && (pluginConfig == null || pluginConfig.isActive(name, dynamic))) {
                    installed.add(plugins.computeIfAbsent(name, n -> createPlugin(file, dynamic)));
                }
            }
        }
        return installed;
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
                    Resetter resetter = byteSupplier.create().append(plugin).install(context.getInstrumentation());
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
     * @param plugins The list of plugins to be enhanced.
     * @return true if the enhancement process was successful, false otherwise.
     */
    private boolean enhanceStatic(List<Plugin> plugins) {
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
                    builder.install(context.getInstrumentation());
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
     * @param dynamic A flag indicating whether the plugin is dynamic.
     * @return The created Plugin instance.
     */
    private Plugin createPlugin(File file, boolean dynamic) {
        URL[] urls = context.getAgentPath().getLibUrls(file);
        ClassLoader classLoader = supervisor.create(file.getName(), urls);
        Plugin plugin = new Plugin(file, dynamic, urls, extensionManager.build(PluginDefinition.class, classLoader));
        plugin.addListener(pluginListener);
        return plugin;
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
     * Removes a plugin from the system. This involves differentiating between dynamic and static plugins:
     * - Removing dynamic plugins from the dynamic plugins collection.
     * - Removing static plugins from the static plugins collection.
     *
     * @param plugin The plugin to be removed.
     */
    private void remove(Plugin plugin) {
        if (plugin.isDynamic()) {
            dynamics.remove(plugin.getName());
        } else {
            statics.remove(plugin.getName());
        }
    }

    @Override
    public synchronized void uninstall() {
        uninstall(dynamics.keySet());
    }

    @Override
    public synchronized void uninstall(Set<String> names) {
        if (names != null) {
            for (String name : names) {
                if (name != null && !name.isEmpty()) {
                    Plugin plugin = dynamics.remove(name);
                    if (plugin != null) {
                        plugin.uninstall();
                    }
                }
            }
        }
    }

}
