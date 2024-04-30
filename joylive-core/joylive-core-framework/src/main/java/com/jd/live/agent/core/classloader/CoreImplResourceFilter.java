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

import com.jd.live.agent.bootstrap.classloader.ResourceConfig;
import com.jd.live.agent.bootstrap.classloader.ResourceConfigFilter;

import java.io.File;

/**
 * The CoreImplResourceFilter class extends the functionality of a ResourceConfigFilter,
 * providing a specific implementation for filtering core implementation resources.
 *
 * @see ResourceConfigFilter
 */
public class CoreImplResourceFilter extends ResourceConfigFilter {

    /**
     * Constructs a CoreImplResourceFilter with a specific ResourceConfig and a path to the configuration file.
     * If the provided ResourceConfig is null, the filter will use a default configuration for core implementation resources.
     *
     * @param config     The ResourceConfig to use for filtering operations, or null to use the default.
     * @param configPath The File object representing the path to the configuration file.
     */
    public CoreImplResourceFilter(ResourceConfig config, File configPath) {
        super(config == null ? ResourceConfig.DEFAULT_CORE_IMPL_RESOURCE_CONFIG : config, configPath);
    }

    /**
     * Constructs a CoreImplResourceFilter with a default resource configuration for core implementation resources and a path to the configuration file.
     *
     * @param configPath The File object representing the path to the configuration file.
     */
    public CoreImplResourceFilter(File configPath) {
        super(ResourceConfig.DEFAULT_CORE_IMPL_RESOURCE_CONFIG, configPath);
    }
}

