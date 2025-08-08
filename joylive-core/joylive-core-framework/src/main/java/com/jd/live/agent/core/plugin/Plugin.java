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

import com.jd.live.agent.bootstrap.classloader.LiveClassLoader;
import com.jd.live.agent.core.bytekit.type.TypeDesc;
import com.jd.live.agent.core.extension.ExtensibleLoader;
import com.jd.live.agent.core.extension.condition.ConditionMatcher;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import lombok.Builder;
import lombok.Getter;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.jd.live.agent.core.util.CollectionUtils.add;
import static com.jd.live.agent.core.util.StringUtils.getPackage;

/**
 * Represents a plugin with capabilities to load, manage, and interact with plugin definitions.
 * A plugin is identified by its name and path, and can be dynamic or static. It maintains a lifecycle
 * status and notifies listeners about relevant events.
 *
 */
public class Plugin extends AbstractPluginDescriptor {

    /**
     * The file path of the plugin's location.
     */
    @Getter
    private final File path;

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
     * Constructs a new Plugin with specified parameters.
     *
     * @param path             The file path of the plugin's location.
     * @param name             The name of the plugin.
     * @param type             The type of the plugin.
     * @param urls             An array of {@link URL} objects for class loading.
     * @param loader           The class loader to use for loading plugin definitions.
     * @param conditionMatcher The condition matcher used to determine if a plugin definition should be
     * applied based on the current execution context.
     */
    @Builder
    public Plugin(File path,
                  String name,
                  PluginType type,
                  URL[] urls,
                  ExtensibleLoader<PluginDefinition> loader,
                  ConditionMatcher conditionMatcher) {
        super(name, type, conditionMatcher, null);
        this.path = path;
        this.urls = urls;
        this.loader = loader;
    }

    @Override
    public List<PluginDefinition> match(TypeDesc typeDesc, ClassLoader classLoader) {
        List<PluginDefinition> result = new ArrayList<>(4);
        if (typeDesc != null) {
            add(definitions, result, p -> match(typeDesc, p, classLoader));
        }
        return result;
    }

    @Override
    protected boolean doLoad() throws Exception {
        definitions = loader.loadExtensible().getExtensions();
        ClassLoader classLoader = loader.getClassLoader();
        if (!definitions.isEmpty() && classLoader instanceof LiveClassLoader) {
            // add package to class loader to faster class loading
            ((LiveClassLoader) classLoader).addPackage(getPackage(definitions.get(0).getClass().getPackage().getName()));
        }
        return true;
    }

    @Override
    protected void doRelease(Consumer<ClassLoader> recycler) {
        if (loader != null) {
            recycler.accept(loader.getClassLoader());
        }
    }

}
