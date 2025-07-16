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
package com.jd.live.agent.bootstrap.classloader;

import com.jd.live.agent.bootstrap.util.Inclusion;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

@Setter
@Getter
public class ResourceConfig {
    public static final String CORE_PREFIX = "classloader.core";
    public static final ResourceConfig DEFAULT_CORE_RESOURCE_CONFIG = new ResourceConfig(
            null,
            new String[]{"yaml", "yml", "xml", "json", "properties"},
            null,
            new String[]{"com.jd.live.agent.bootstrap."},
            new String[]{},
            new String[]{"com.jd.live.agent.core.", "com.jd.live.agent.governance."},
            null,
            new String[]{"META-INF/services/com.jd.live.agent"});
    public static final ResourceConfig DEFAULT_CORE_IMPL_RESOURCE_CONFIG = new ResourceConfig(
            null,
            null,
            null,
            new String[]{"com.jd.live.agent.bootstrap.", "com.jd.live.agent.core.", "com.jd.live.agent.governance."},
            new String[]{},
            new String[]{"com.jd.live.agent.implement.", "com.jd.live.agent.shaded."},
            new String[]{},
            new String[]{}); // can be loaded in the parent class loader
    public static final ResourceConfig DEFAULT_PLUGIN_RESOURCE_CONFIG = new ResourceConfig(
            null,
            null,
            null,
            new String[]{"com.jd.live.agent.bootstrap.", "com.jd.live.agent.core.", "com.jd.live.agent.governance."},
            new String[]{},
            new String[]{"com.jd.live.agent.plugin."},
            null,
            null); // can be loaded in the parent class loader

    private Inclusion config;
    private Inclusion parent;
    private Inclusion self;
    private Inclusion isolation;

    public ResourceConfig() {
        this(null, null, null, null, null, null, null, (Collection<String>) null);
    }

    public ResourceConfig(Function<String, Object> env, String prefix) {
        // use java native method in LiveAgent
        this(parse(env, prefix + ".configResources"),
                parse(env, prefix + ".configExtensions"),
                parse(env, prefix + ".parentResources"),
                parse(env, prefix + ".parentPrefixes"),
                parse(env, prefix + ".selfResources"),
                parse(env, prefix + ".selfPrefixes"),
                parse(env, prefix + ".isolationResources"),
                parse(env, prefix + ".isolationPrefixes"));
    }

    public ResourceConfig(String[] configResources,
                          String[] configExtensions,
                          String[] parentResources,
                          String[] parentPrefixes,
                          String[] selfResources,
                          String[] selfPrefixes,
                          String[] isolationResources,
                          String[] isolationPrefixes) {
        this(configResources == null ? null : Arrays.asList(configResources),
                configExtensions == null ? null : Arrays.asList(configExtensions),
                parentResources == null ? null : Arrays.asList(parentResources),
                parentPrefixes == null ? null : Arrays.asList(parentPrefixes),
                selfResources == null ? null : Arrays.asList(selfResources),
                selfPrefixes == null ? null : Arrays.asList(selfPrefixes),
                isolationResources == null ? null : Arrays.asList(isolationResources),
                isolationPrefixes == null ? null : Arrays.asList(isolationPrefixes));
    }

    public ResourceConfig(Collection<String> configResources,
                          Collection<String> configExtensions,
                          Collection<String> parentResources,
                          Collection<String> parentPrefixes,
                          Collection<String> selfResources,
                          Collection<String> selfPrefixes,
                          Collection<String> isolationResources,
                          Collection<String> isolationPrefixes) {
        config = Inclusion.builder().factory(Inclusion.ContainsPredicateFactory.INSTANCE)
                .addNames(configResources)
                .addPrefixes(configExtensions, String::toLowerCase)
                .build();
        parent = Inclusion.builder().addNames(parentResources).addPrefixes(parentPrefixes).build();
        self = Inclusion.builder().addNames(selfResources).addPrefixes(selfPrefixes).build();
        isolation = Inclusion.builder().addNames(isolationResources).addPrefixes(isolationPrefixes).build();
    }

    public boolean isConfig(String name) {
        return config.test(name, ResourceConfig::getExtension);
    }

    public boolean isParent(String name) {
        return parent.test(name);
    }

    public boolean isSelf(String name) {
        return self.test(name);
    }

    public boolean isIsolation(String name) {
        return isolation.test(name);
    }

    /**
     * Extracts the file extension from a filename (e.g., "file.txt" → "txt").
     *
     * @param name the filename to process
     * @return the lowercase extension or empty string if none
     */
    private static String getExtension(String name) {
        int pos = name.lastIndexOf('.');
        return pos > 0 ? name.substring(pos + 1).toLowerCase() : "";
    }

    /**
     * Parses an environment variable into a set of strings.
     *
     * @param env environment variable accessor
     * @param key variable name to lookup
     * @return set of non-empty values (null if key not found/empty)
     */
    protected static Set<String> parse(Function<String, Object> env, String key) {
        String value = (String) env.apply(key);
        if (value == null || value.isEmpty()) {
            return null;
        }
        String[] parts = value.split("[,;]");
        Set<String> result = new HashSet<>(parts.length);
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                result.add(part);
            }
        }
        return result;
    }

}
