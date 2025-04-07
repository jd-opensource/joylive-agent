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

import com.jd.live.agent.bootstrap.plugin.PluginListener;
import com.jd.live.agent.core.bytekit.matcher.ElementMatcher;
import com.jd.live.agent.core.bytekit.matcher.NameMatcher;
import com.jd.live.agent.core.bytekit.matcher.OneOfMatcher;
import com.jd.live.agent.core.bytekit.matcher.StringMatcher;
import com.jd.live.agent.core.bytekit.type.TypeDesc;
import com.jd.live.agent.core.extension.condition.ConditionMatcher;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.jd.live.agent.core.util.CollectionUtils.add;

/**
 * Represents a composite plugin that contains multiple plugins and manages their lifecycle and matching.
 */
public class CompositePlugin extends AbstractPluginDescriptor {

    private final List<Plugin> plugins;

    private final Map<String, List<PluginDefinition>> namedDefinitions = new ConcurrentHashMap<>();

    private final List<PluginDefinition> otherDefinitions = new ArrayList<>();

    /**
     * Constructs a new instance of {@link CompositePlugin} with the specified parameters.
     *
     * @param name             the name of the plugin composite
     * @param type             the type of the plugin composite
     * @param plugins          the list of plugins to be included in the composite
     * @param conditionMatcher the condition matcher used to evaluate plugin conditions
     * @param pluginListener   the plugin listener to be added (optional, can be null)
     */
    public CompositePlugin(String name,
                           PluginType type,
                           List<Plugin> plugins,
                           ConditionMatcher conditionMatcher,
                           PluginListener pluginListener) {
        super(name, type, conditionMatcher, pluginListener);
        this.plugins = plugins;
    }

    @Override
    public List<PluginDefinition> match(TypeDesc typeDesc, ClassLoader classLoader) {
        List<PluginDefinition> result = new ArrayList<>(4);
        if (typeDesc != null) {
            add(namedDefinitions.get(typeDesc.getActualName()), result, p -> isEnabled(p, classLoader));
            add(otherDefinitions, result, p -> match(typeDesc, p, classLoader));
        }
        return result;
    }

    @Override
    public boolean load() {
        return load(e -> fail("failed to load plugin " + getName(), e));
    }

    @Override
    protected boolean doLoad() {
        List<PluginDefinition> definitions = new ArrayList<>();
        for (Plugin plugin : plugins) {
            if (plugin.load(e -> fail("failed to load plugin " + plugin.getName(), e))) {
                if (!plugin.isEmpty()) {
                    definitions.addAll(plugin.getDefinitions());
                }
            } else {
                return false;
            }
        }
        this.definitions = definitions;
        sort();
        return true;
    }


    /**
     * Sorts the plugin definitions into named and other definitions based on the type of matcher.
     * Named definitions that use a fully equal string matcher are stored in a map, while other definitions
     * are stored in a separate list.
     */
    private void sort() {
        for (PluginDefinition definition : definitions) {
            ElementMatcher<?> matcher = definition.getMatcher();
            if (matcher instanceof NameMatcher) {
                matcher = ((NameMatcher<?>) matcher).getMatcher();
                if (matcher instanceof StringMatcher) {
                    StringMatcher stringMatcher = (StringMatcher) matcher;
                    if (stringMatcher.getMode() == StringMatcher.OperationMode.EQUALS_FULLY) {
                        namedDefinitions.computeIfAbsent(stringMatcher.getValue(), k -> new ArrayList<>()).add(definition);
                        continue;
                    }
                } else if (matcher instanceof OneOfMatcher) {
                    OneOfMatcher oneOfMatcher = (OneOfMatcher) matcher;
                    oneOfMatcher.getValues().forEach(value ->
                            namedDefinitions.computeIfAbsent(value, k -> new ArrayList<>()).add(definition));
                    continue;
                }
            }
            otherDefinitions.add(definition);
        }
    }

    @Override
    protected void doUninstall() {
        for (Plugin plugin : plugins) {
            plugin.uninstall(null);
        }
    }

    @Override
    protected void doRelease(Consumer<ClassLoader> recycler) {
        for (Plugin plugin : plugins) {
            plugin.doRelease(recycler);
        }
    }
}
