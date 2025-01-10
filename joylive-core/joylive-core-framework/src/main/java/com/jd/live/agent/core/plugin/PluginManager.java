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
import com.jd.live.agent.bootstrap.classloader.URLResourcer;
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
import com.jd.live.agent.core.extension.ExtensibleLoader;
import com.jd.live.agent.core.extension.ExtensionManager;
import com.jd.live.agent.core.extension.condition.ConditionMatcher;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.util.Close;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages the lifecycle of plugins within an agent context. This includes installing, enhancing,
 * and uninstalling both static and dynamic plugins.
 *
 * @since 1.0.0
 */
public class PluginManager implements PluginSupervisor {
    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);
    private static final String SYSTEM_PLUGIN = "system";
    private static final String STATIC_PLUGIN = "static";

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

    private final ConditionMatcher conditionMatcher;

    /**
     * A map storing installed plugins.
     */
    private final Map<String, PluginDescriptor> installed = new ConcurrentHashMap<>();

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
                         ByteSupplier byteSupplier,
                         ConditionMatcher conditionMatcher) {

        this.instrumentation = instrumentation;
        this.pluginConfig = pluginConfig == null ? new PluginConfig() : pluginConfig;
        this.agentPath = agentPath;
        this.extensionManager = extensionManager;
        this.supervisor = supervisor;
        this.byteSupplier = byteSupplier;
        this.conditionMatcher = conditionMatcher;
    }

    @Override
    public synchronized boolean install(boolean dynamic) {
        if (!enhanceSystem(install(pluginConfig.getSystems(), PluginType.SYSTEM))) {
            // install system plugins.
            return false;
        } else if (!enhanceStatic(install(pluginConfig.getStatics(), PluginType.STATIC))) {
            // install static plugins.
            return false;
        } else if (dynamic) {
            return enhanceDynamic(install(pluginConfig.getDynamics(), PluginType.DYNAMIC));
        }
        return true;

    }

    @Override
    public synchronized boolean install(Set<String> names) {
        return enhanceDynamic(install(names, PluginType.DYNAMIC));
    }

    @Override
    public synchronized void uninstall() {
        for (PluginDescriptor plugin : installed.values()) {
            uninstall(plugin);
        }
    }

    @Override
    public synchronized void uninstall(Set<String> names) {
        if (names != null) {
            for (String name : names) {
                if (name != null && !name.isEmpty()) {
                    uninstall(installed.get(name));
                }
            }
        }
    }

    /**
     * Installs plugins based on the given set of plugin names and the dynamic flag.
     *
     * @param names   the set of plugin names to install
     * @return a list of plugins to install
     */
    private List<PluginDescriptor> install(Set<String> names, PluginType type) {
        List<PluginDescriptor> result = new LinkedList<>();
        if (names == null || names.isEmpty()) {
            return result;
        }
        PluginDescriptor plugin;
        List<File> actives = getActives(names, type);
        if (type == PluginType.SYSTEM) {
            plugin = installed.computeIfAbsent(SYSTEM_PLUGIN, n ->
                    new CompositePlugin(SYSTEM_PLUGIN, type, createPlugins(actives, type), conditionMatcher, pluginListener));
            addPlugin(plugin, result);
        } else if (type == PluginType.STATIC) {
            plugin = installed.computeIfAbsent(STATIC_PLUGIN, n ->
                    new CompositePlugin(STATIC_PLUGIN, type, createPlugins(actives, type), conditionMatcher, pluginListener));
            addPlugin(plugin, result);
        } else if (type == PluginType.DYNAMIC) {
            for (File file : actives) {
                plugin = installed.computeIfAbsent(file.getName(), n -> createPlugin(file, type, pluginListener));
                addPlugin(plugin, result);
            }
        }
        return result;
    }

    /**
     * Adds a plugin to the result list if its status is CREATED, FAILED, or LOADED.
     *
     * @param plugin the plugin descriptor to add
     * @param result the list to which the plugin will be added if conditions are met
     */
    private void addPlugin(PluginDescriptor plugin, List<PluginDescriptor> result) {
        switch (plugin.getStatus()) {
            case CREATED:
            case FAILED:
            case LOADED:
                result.add(plugin);
                break;
        }
    }

    /**
     * Retrieves a list of active plugin files based on the given names and plugin type.
     *
     * @param names the set of plugin names to check
     * @param type  the type of the plugins
     * @return a list of active plugin files
     */
    private List<File> getActives(Set<String> names, PluginType type) {
        List<File> actives = new ArrayList<>(names.size());
        Map<String, File> files = agentPath.getPlugins();
        for (String name : names) {
            File file = files.get(name);
            if (file != null && isActive(name, type)) {
                actives.add(file);
            }
        }
        return actives;
    }

    /**
     * Checks if a plugin is active based on its name and type.
     *
     * @param name the name of the plugin
     * @param type the type of the plugin
     * @return {@code true} if the plugin is active, otherwise {@code false}
     */
    private boolean isActive(String name, PluginType type) {
        switch (type) {
            case SYSTEM:
                return pluginConfig.isSystemActive(name);
            case STATIC:
                return pluginConfig.isStaticActive(name);
            case DYNAMIC:
                return pluginConfig.isDynamicActive(name);
        }
        return false;
    }

    /**
     * Creates a list of plugins from the given list of active files and plugin type.
     *
     * @param actives the list of active plugin files
     * @param type    the type of the plugins
     * @return a list of created plugins
     */
    private List<Plugin> createPlugins(List<File> actives, PluginType type) {
        List<Plugin> result = new LinkedList<>();
        actives.forEach(plugin -> result.add(createPlugin(plugin, type, null)));
        return result;
    }

    /**
     * Enhances the given list of plugins.
     *
     * @param plugins the list of plugins to enhance
     * @return true if the enhancement is successful, false otherwise
     */
    private boolean enhance(List<PluginDescriptor> plugins, Function<List<PluginDescriptor>, Boolean> enhancer) {
        if (plugins == null || plugins.isEmpty()) {
            return true;
        }
        // a transaction
        boolean success = loadPlugins(plugins);
        List<PluginDescriptor> enhances = plugins;
        if (success) {
            enhances = plugins.stream().filter(p -> p.getStatus() == PluginStatus.LOADED && !p.isEmpty()).collect(Collectors.toList());
            success = enhancer.apply(enhances);
        }
        if (!success) {
            logger.info("Start uninstalling plugins when an exception occurs");
            enhances.forEach(PluginDescriptor::uninstall);
        } else {
            enhances.forEach(PluginDescriptor::success);
        }
        return success;
    }

    /**
     * Enhances the given list of plugins dynamically.
     *
     * @param plugins the list of plugins to enhance
     * @return true if the enhancement is successful, false otherwise
     */
    private boolean enhanceDynamic(List<PluginDescriptor> plugins) {
        return enhance(plugins, loads -> {
            for (PluginDescriptor plugin : loads) {
                try {
                    Resetter resetter = byteSupplier.create().append(plugin).install(instrumentation);
                    plugin.addListener(p -> {
                        if (p.getType() == EventType.UNINSTALL) {
                            resetter.reset();
                        }
                    });
                } catch (Throwable e) {
                    plugin.fail("Failed to enhance plugin " + plugin.getName(), e);
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * Enhances system plugins by loading and installing them if they meet certain conditions.
     *
     * @param plugins the list of plugins to be enhanced.
     * @return true if the enhancement process was successful, false otherwise.
     */
    private boolean enhanceSystem(List<PluginDescriptor> plugins) {
        return enhanceStatic(plugins);
    }

    /**
     * Enhances static plugins by loading and installing them if they meet certain conditions.
     *
     * @param plugins the list of plugins to be enhanced.
     * @return true if the enhancement process was successful, false otherwise.
     */
    private boolean enhanceStatic(List<PluginDescriptor> plugins) {
        return enhance(plugins, loads -> {
            ByteBuilder builder = byteSupplier.create();
            for (PluginDescriptor plugin : loads) {
                builder = builder.append(plugin);
            }
            try {
                builder.install(instrumentation);
            } catch (Throwable e) {
                logger.error("Failed to enhance plugins", e);
                return false;
            }
            return true;
        });
    }

    /**
     * Creates a new Plugin instance from a given file.
     *
     * @param file    The file from which the plugin is created.
     * @param type    The plugin type.
     * @return The created Plugin instance.
     */
    private Plugin createPlugin(File file, PluginType type, PluginListener listener) {
        String name = file.getName();
        URL[] urls = agentPath.getLibUrls(file);
        ClassLoader classLoader = supervisor.create(name, urls);
        ExtensibleLoader<PluginDefinition> loader = extensionManager.build(PluginDefinition.class, classLoader);
        Plugin plugin = Plugin.builder()
                .path(file)
                .name(name)
                .type(type)
                .urls(urls)
                .loader(loader)
                .conditionMatcher(conditionMatcher)
                .build();
        if (listener != null) {
            plugin.addListener(listener);
        }
        return plugin;
    }

    /**
     * Loads a list of plugins by invoking their load method.
     * If any plugin fails to load, the process is halted and false is returned.
     *
     * @param plugins The list of plugins to be loaded.
     * @return true if all plugins are loaded successfully, false if any plugin fails to load.
     */
    private boolean loadPlugins(List<PluginDescriptor> plugins) {
        for (PluginDescriptor plugin : plugins) {
            plugin.load();
            if (plugin.getStatus() == PluginStatus.FAILED) {
                return false;
            }
        }
        return true;
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
        PluginDescriptor plugin = event.getOwner();
        remove(plugin);
        plugin.release(loader -> {
            supervisor.remove(loader instanceof URLResourcer ? ((URLResourcer) loader).getId() : null);
            extensionManager.remove(loader);
            Close.instance().close(loader instanceof AutoCloseable ? (AutoCloseable) loader : null);
        });
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
    private void remove(PluginDescriptor plugin) {
        installed.remove(plugin.getName());
    }

    private void uninstall(PluginDescriptor plugin) {
        // Only dynamic plugins with the specified name can be deleted
        if (plugin != null && plugin.getType() == PluginType.DYNAMIC) {
            remove(plugin);
            plugin.uninstall();
        }
    }

}
