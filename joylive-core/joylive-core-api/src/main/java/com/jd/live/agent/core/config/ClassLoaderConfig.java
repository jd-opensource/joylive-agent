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
package com.jd.live.agent.core.config;

import com.jd.live.agent.bootstrap.classloader.ResourceConfig;
import com.jd.live.agent.bootstrap.util.Inclusion;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Configurable;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.HashSet;

@Getter
@Setter
@Configurable(prefix = "classloader")
public class ClassLoaderConfig {

    public static final String COMPONENT_CLASSLOADER_CONFIG = "classLoaderConfig";

    @Config("core")
    private ResourceConfig coreResource = ResourceConfig.DEFAULT_CORE_RESOURCE_CONFIG;

    @Config("coreImpl")
    private ResourceConfig coreImplResource = ResourceConfig.DEFAULT_CORE_IMPL_RESOURCE_CONFIG;

    @Config("plugin")
    private ResourceConfig pluginResource = ResourceConfig.DEFAULT_PLUGIN_RESOURCE_CONFIG;

    @Config("essential")
    private EssentialConfig essentialResource = new EssentialConfig();

    public boolean isEssential(String name) {
        return essentialResource.isEssential(name);
    }

    @Getter
    @Setter
    public static class EssentialConfig {

        public static final Inclusion DEFAULT_ESSENTIAL_INCLUSION = new Inclusion(null, new HashSet<>(Collections.singletonList("com.jd.live.agent.")));
        public static final Inclusion DEFAULT_ESSENTIAL_EXCLUSION = new Inclusion(null, new HashSet<>(Collections.singletonList("com.jd.live.agent.demo.")));
        public static final Inclusion DEFAULT_ESSENTIAL_BOOTSTRAP = new Inclusion(null, new HashSet<>(Collections.singletonList("com.jd.live.agent.bootstrap.")));

        private Inclusion inclusion = DEFAULT_ESSENTIAL_INCLUSION;
        private Inclusion exclusion = DEFAULT_ESSENTIAL_EXCLUSION;
        private Inclusion bootstrap = DEFAULT_ESSENTIAL_BOOTSTRAP;

        public boolean isEssential(String name) {
            return !exclusion.test(name) && inclusion.test(name);
        }
    }
}
