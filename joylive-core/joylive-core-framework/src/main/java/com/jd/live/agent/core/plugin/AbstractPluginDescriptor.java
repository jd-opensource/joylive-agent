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
import com.jd.live.agent.core.bytekit.type.TypeDesc;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.extension.condition.ConditionMatcher;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.util.Executors;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static com.jd.live.agent.core.extension.condition.ConditionMatcher.DEPEND_ON_LOADER;

/**
 * Represents a composite plugin that contains multiple plugins and manages their lifecycle and matching.
 */
public abstract class AbstractPluginDescriptor implements PluginDescriptor {

    /**
     * The name of the plugin.
     */
    @Getter
    protected final String name;

    /**
     * Indicates whether the plugin is dynamic.
     */
    @Getter
    protected final PluginType type;

    protected final ConditionMatcher conditionMatcher;

    /**
     * A thread-safe list of plugin listeners to notify about plugin events.
     */
    protected final List<PluginListener> listeners = new CopyOnWriteArrayList<>();

    protected final Map<String, Boolean> enabled = new ConcurrentHashMap<>();

    @Getter
    protected PluginStatus status = PluginStatus.CREATED;

    @Getter
    protected List<PluginDefinition> definitions = new ArrayList<>(0);

    /**
     * Constructs a new instance of {@link AbstractPluginDescriptor} with the specified parameters.
     *
     * @param name             the name of the plugin composite
     * @param type             the type of the plugin composite
     * @param conditionMatcher the condition matcher used to evaluate plugin conditions
     * @param pluginListener   the plugin listener to be added (optional, can be null)
     */
    public AbstractPluginDescriptor(String name,
                                    PluginType type,
                                    ConditionMatcher conditionMatcher,
                                    PluginListener pluginListener) {
        this.name = name;
        this.type = type;
        this.conditionMatcher = conditionMatcher;
        if (pluginListener != null) {
            addListener(pluginListener);
        }
    }

    @Override
    public void addListener(PluginListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public boolean load() {
        return load(e -> fail("failed to load plugin " + name, e));
    }

    /**
     * Loads the plugin by performing the actual loading logic defined in {@link #doLoad()}.
     *
     * @param errorHandler a consumer that can be used to handle exceptions during the loading process (can be null)
     * @return {@code true} if the plugin is successfully loaded, otherwise {@code false}
     */
    protected boolean load(Consumer<Throwable> errorHandler) {
        if (status == PluginStatus.CREATED) {
            try {
                status = doLoad() ? PluginStatus.LOADED : PluginStatus.FAILED;
            } catch (Throwable e) {
                status = PluginStatus.FAILED;
                if (errorHandler != null) {
                    errorHandler.accept(e);
                }
                return false;
            }
        }
        return status == PluginStatus.LOADED;
    }

    /**
     * Uninstalls the plugin by setting its status back to LOADED and publishes an uninstall event.
     */
    public void uninstall() {
        uninstall(p -> publish(new PluginEvent(p, EventType.UNINSTALL, "Uninstall plugin " + name)));
    }

    /**
     * Uninstalls the plugin by setting its status back to LOADED and performs additional uninstall logic.
     *
     * @param consumer a consumer that can be used to perform additional actions after uninstallation (can be null)
     */
    protected void uninstall(Consumer<PluginDescriptor> consumer) {
        if (status == PluginStatus.SUCCESS) {
            status = PluginStatus.LOADED;
        } else if (status == PluginStatus.FAILED) {
            status = definitions == null ? PluginStatus.CREATED : PluginStatus.LOADED;
        }
        doUninstall();
        if (consumer != null) {
            consumer.accept(this);
        }
    }

    @Override
    public void release(Consumer<ClassLoader> recycler) {
        doRelease(recycler);
    }

    /**
     * Marks the plugin as successfully loaded and publishes a success event to all listeners.
     */
    public void success() {
        if (status == PluginStatus.LOADED) {
            status = PluginStatus.SUCCESS;
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
        status = PluginStatus.FAILED;
        publish(new PluginEvent(this, EventType.FAIL, message, throwable));
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
     * Performs the actual loading logic for the plugin.
     *
     * @return {@code true} if the loading is successful, otherwise {@code false}
     * @throws Exception if an error occurs during the loading process
     */
    protected boolean doLoad() throws Exception {
        return true;
    }

    /**
     * Performs the actual uninstall logic for the plugin.
     */
    protected void doUninstall() {

    }

    /**
     * Releases resources associated with the plugin.
     *
     * @param recycler a recycler that can be used to release resources related to the class loader (can be null)
     */
    protected void doRelease(Consumer<ClassLoader> recycler) {

    }

    /**
     * Checks if the given plugin definition matches the specified type description and is enabled for the given class loader.
     *
     * @param typeDesc    the type description to match against
     * @param definition  the plugin definition to check
     * @param classLoader the class loader to check against
     * @return {@code true} if the plugin definition matches and is enabled, otherwise {@code false}
     */
    protected boolean match(TypeDesc typeDesc, PluginDefinition definition, ClassLoader classLoader) {
        if (!isEnabled(definition, classLoader)) {
            return false;
        }
        // The previous call has already ensured that the class exists.
        // Set the class loader for the current thread.
        try {
            return Executors.execute(classLoader, () -> definition.getMatcher().match(typeDesc));
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Checks if the given plugin definition is enabled for the specified class loader.
     * This method uses a cache to store the results of the checks to improve performance.
     *
     * @param definition  the plugin definition to check
     * @param classLoader the class loader to check against
     * @return {@code true} if the plugin definition is enabled for the class loader, otherwise {@code false}
     */
    protected boolean isEnabled(PluginDefinition definition, ClassLoader classLoader) {
        Class<?> type = definition.getClass();
        String name = type.getName();
        name = classLoader == null ? name : (name + "@" + classLoader);
        return enabled.computeIfAbsent(name, n -> conditionMatcher.match(type, classLoader, DEPEND_ON_LOADER));
    }

}
