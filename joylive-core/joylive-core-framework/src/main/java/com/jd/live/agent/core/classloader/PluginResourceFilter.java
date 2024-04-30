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
package com.jd.live.agent.core.classloader;

import com.jd.live.agent.bootstrap.classloader.CandidatorOption;
import com.jd.live.agent.bootstrap.classloader.ResourceConfig;
import com.jd.live.agent.bootstrap.classloader.ResourceConfigFilter;

import java.io.File;

/**
 * The PluginResourceFilter class extends the functionality of a ResourceConfigFilter,
 * providing a specific implementation for filtering plugin resources.
 *
 * @see ResourceConfigFilter
 */
public class PluginResourceFilter extends ResourceConfigFilter {

    /**
     * Constructs a PluginResourceFilter with a specific ResourceConfig and a path to the configuration file.
     *
     * @param config     The ResourceConfig to use for filtering operations.
     * @param configPath The File object representing the path to the configuration file.
     */
    public PluginResourceFilter(ResourceConfig config, File configPath) {
        super(config, configPath);
    }

    /**
     * Constructs a PluginResourceFilter with a default resource configuration for plugins and a path to the configuration file.
     *
     * @param configPath The File object representing the path to the configuration file.
     */
    public PluginResourceFilter(File configPath) {
        super(ResourceConfig.DEFAULT_PLUGIN_RESOURCE_CONFIG, configPath);
    }

    /**
     * Retrieves the ClassLoader to be used by the filter.
     * This method checks the CandidatorOption for an associated ClassLoader.
     *
     * @return The ClassLoader to be used, or null if no ClassLoader is specified in the CandidatorOption.
     */
    @Override
    public ClassLoader getCandidator() {
        CandidatorOption option = CandidatorOption.getOption();
        return option == null ? null : option.getClassLoader();
    }
}

