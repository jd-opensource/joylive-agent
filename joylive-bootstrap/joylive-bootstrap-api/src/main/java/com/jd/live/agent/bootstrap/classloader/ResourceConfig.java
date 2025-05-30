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
                    "com.jd.live.agent.bootstrap.",
            },
            new String[]{},
            new String[]{
                    "com.jd.live.agent.core.",
                    "com.jd.live.agent.governance."
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
            },
            new String[]{},
            new String[]{
                    "com.jd.live.agent.implement.",
                    "com.jd.live.agent.shaded."
            },
            new String[]{},
            new String[]{}); // can be loaded in the parent class loader
    public static final ResourceConfig DEFAULT_PLUGIN_RESOURCE_CONFIG = new ResourceConfig(
            null,
            null,
            null,
            new String[]{
                    "com.jd.live.agent.bootstrap.",
                    "com.jd.live.agent.core.",
                    "com.jd.live.agent.governance.",
            },
            new String[]{},
            new String[]{"com.jd.live.agent.plugin."},
            null,
            null); // can be loaded in the parent class loader

    private Inclusion config;
    private Inclusion parent;
    private Inclusion self;
    private Inclusion isolation;

    public ResourceConfig() {
    }

    public ResourceConfig(Function<String, Object> env, String prefix) {
        // use java native method in LiveAgent
        config = new Inclusion(parse(env, prefix + ".configResources"), parse(env, prefix + ".configExtensions"));
        parent = new Inclusion(parse(env, prefix + ".parentResources"), parse(env, prefix + ".parentPrefixes"));
        self = new Inclusion(parse(env, prefix + ".selfResources"), parse(env, prefix + ".selfPrefixes"));
        isolation = new Inclusion(parse(env, prefix + ".isolationResources"), parse(env, prefix + ".isolationPrefixes"));
    }

    public ResourceConfig(String[] configResources,
                          String[] configExtensions,
                          String[] parentResources,
                          String[] parentPrefixes,
                          String[] selfResources,
                          String[] selfPrefixes,
                          String[] isolationResources,
                          String[] isolationPrefixes) {
        config = new Inclusion(configResources == null ? null : new HashSet<>(Arrays.asList(configResources)),
                configExtensions == null ? null : new HashSet<>(Arrays.asList(configExtensions)));
        parent = new Inclusion(parentResources == null ? null : new HashSet<>(Arrays.asList(parentResources)),
                parentPrefixes == null ? null : new HashSet<>(Arrays.asList(parentPrefixes)));
        self = new Inclusion(selfResources == null ? null : new HashSet<>(Arrays.asList(selfResources)),
                selfPrefixes == null ? null : new HashSet<>(Arrays.asList(selfPrefixes)));
        isolation = new Inclusion(isolationResources == null ? null : new HashSet<>(Arrays.asList(isolationResources)),
                isolationPrefixes == null ? null : new HashSet<>(Arrays.asList(isolationPrefixes)));
    }

    public boolean isConfig(String name) {
        if (name == null) {
            return false;
        } else {
            int pos = name.lastIndexOf('.');
            String extension = pos > 0 ? name.substring(pos + 1) : "";
            return config.test(name, extension::equalsIgnoreCase);
        }
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
