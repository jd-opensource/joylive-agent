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

import com.jd.live.agent.bootstrap.classloader.LiveClassLoader;
import com.jd.live.agent.bootstrap.classloader.ResourceFilter;
import com.jd.live.agent.core.config.ClassLoaderConfig;
import com.jd.live.agent.core.config.AgentPath;
import com.jd.live.agent.core.util.Close;
import lombok.Getter;

import java.net.URL;

import static com.jd.live.agent.bootstrap.classloader.ResourcerType.CORE_IMPL;
import static com.jd.live.agent.bootstrap.classloader.ResourcerType.PLUGIN;

/**
 * ClassLoaderManager is responsible for managing the class loaders used in the application
 *
 * @since 1.0.0
 */
@Getter
public class ClassLoaderManager {

    /**
     * The core class loader used for loading core classes.
     */
    private final LiveClassLoader coreLoader;

    /**
     * The class loader used for loading core implementation classes.
     */
    private final LiveClassLoader coreImplLoader;

    /**
     * The manager responsible for managing plugin class loaders.
     */
    private final PluginLoaderManager pluginLoaders;

    public ClassLoaderManager(LiveClassLoader coreLoader, ClassLoaderConfig loaderConfig, AgentPath agentPath) {
        this.coreLoader = coreLoader;
        URL[] implLibs = agentPath.getLibUrls(agentPath.getCoreImplLibPath());
        ResourceFilter implFilter = new CoreImplResourceFilter(loaderConfig.getCoreImplResource(), agentPath.getConfigPath());
        this.coreImplLoader = new LiveClassLoader(implLibs, coreLoader, CORE_IMPL, implFilter);
        this.pluginLoaders = new PluginLoaderManager((name, urls) -> {
            ResourceFilter filter = new PluginResourceFilter(loaderConfig.getPluginResource(), agentPath.getConfigPath());
            return new LiveClassLoader(urls, coreLoader, PLUGIN, filter, PLUGIN.getName() + "-" + name);
        });
    }

    public void close() {
        Close.instance().close(coreImplLoader).close(pluginLoaders);
    }
}
