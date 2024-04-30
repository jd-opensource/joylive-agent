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

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

@Setter
@Getter
public class ResourceConfig {
    public static final String CORE_PREFIX = "classloader.core";
    public static final ResourceConfig DEFAULT_CORE_RESOURCE_CONFIG = new ResourceConfig(
            null,
            new String[]{
                    "yaml",
                    "yml",
                    "xml",
                    "json",
                    "properties"
            },
            null,
            new String[]{
                    "com.jd.live.agent.bootstrap",
                    "java.",
                    "javax.",
                    "sun."
            },
            null,
            new String[]{
                    "META-INF/services/com.jd.live.agent"
            });
    public static final ResourceConfig DEFAULT_CORE_IMPL_RESOURCE_CONFIG = new ResourceConfig(
            null,
            null,
            null,
            new String[]{
                    "com.jd.live.agent.bootstrap.",
                    "com.jd.live.agent.core.",
                    "com.jd.live.agent.governance.",
                    "java.",
                    "javax.",
                    "sun."
            },
            new String[]{},
            new String[]{}); // can be loaded in the parent class loader
    public static final ResourceConfig DEFAULT_PLUGIN_RESOURCE_CONFIG = new ResourceConfig(
            null,
            null,
            null,
            new String[]{
                    "com.jd.live.agent.bootstrap",
                    "com.jd.live.agent.core.",
                    "com.jd.live.agent.governance",
                    "java.",
                    "javax.",
                    "sun."
            },
            null,
            null); // can be loaded in the parent class loader

    private Set<String> configResources;
    private Set<String> configExtensions;
    private Set<String> parentResources;
    private Set<String> parentPrefixes;
    private Set<String> isolationResources;
    private Set<String> isolationPrefixes;

    public ResourceConfig() {
    }

    public ResourceConfig(Function<String, Object> env, String prefix) {
        // use java native method in LiveAgent
        configResources = parse(env, prefix + ".configResources");
        configExtensions = parse(env, prefix + ".configExtensions");
        parentResources = parse(env, prefix + ".parentResources");
        parentPrefixes = parse(env, prefix + ".parentPrefixes");
        isolationResources = parse(env, prefix + ".isolationResources");
        isolationPrefixes = parse(env, prefix + ".isolationPrefixes");
    }

    public ResourceConfig(Set<String> configResources, Set<String> configExtensions, Set<String> parentResources,
                          Set<String> parentPrefixes, Set<String> isolationResources, Set<String> isolationPrefixes) {
        this.configResources = configResources;
        this.configExtensions = configExtensions;
        this.parentResources = parentResources;
        this.parentPrefixes = parentPrefixes;
        this.isolationResources = isolationResources;
        this.isolationPrefixes = isolationPrefixes;
    }

    public ResourceConfig(String[] configResources, String[] configExtensions, String[] parentResources,
                          String[] parentPrefixes, String[] isolationResources, String[] isolationPrefixes) {
        this.configResources = configResources == null ? null : new HashSet<>(Arrays.asList(configResources));
        this.configExtensions = configExtensions == null ? null : new HashSet<>(Arrays.asList(configExtensions));
        this.parentResources = parentResources == null ? null : new HashSet<>(Arrays.asList(parentResources));
        this.parentPrefixes = parentPrefixes == null ? null : new HashSet<>(Arrays.asList(parentPrefixes));
        this.isolationResources = isolationResources == null ? null : new HashSet<>(Arrays.asList(isolationResources));
        this.isolationPrefixes = isolationPrefixes == null ? null : new HashSet<>(Arrays.asList(isolationPrefixes));
    }

    public boolean isConfig(String name) {
        if (name == null) {
            return false;
        } else {
            String extension = "";
            int pos = name.lastIndexOf('.');
            if (pos > 0) {
                extension = name.substring(pos + 1);
            }
            return contains(configResources, configExtensions, name, extension::equalsIgnoreCase);
        }
    }

    public boolean isParent(String name) {
        return name != null && contains(parentResources, parentPrefixes, name, name::startsWith);
    }

    public boolean isIsolation(String name) {
        return name != null && contains(isolationResources, isolationPrefixes, name, name::startsWith);
    }

    protected boolean contains(Set<String> resources, Set<String> others,
                               String name, Function<String, Boolean> func) {
        boolean result = false;
        if (resources != null) {
            result = resources.contains(name);
        }
        if (!result && others != null) {
            for (String other : others) {
                result = !other.isEmpty() && func.apply(other);
                if (result) {
                    break;
                }
            }
        }
        return result;
    }

    protected Set<String> parse(Function<String, Object> env, String key) {
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
