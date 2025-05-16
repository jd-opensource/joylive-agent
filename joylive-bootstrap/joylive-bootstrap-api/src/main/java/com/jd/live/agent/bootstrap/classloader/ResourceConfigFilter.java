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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

public class ResourceConfigFilter implements ResourceFilter {


    private final ResourceConfig config;

    private final File configPath;

    public ResourceConfigFilter(ResourceConfig config, File configPath) {
        this.config = config;
        this.configPath = configPath;
    }

    @Override
    public ClassLoader getCandidator() {
        return null;
    }

    @Override
    public boolean loadByParent(String name) {
        return config.isParent(name);
    }

    @Override
    public boolean loadBySelf(String name) {
        return config.isSelf(name);
    }

    @Override
    public URL getResource(String name, ResourceFinder resourcer) {
        if (config.isConfig(name)) {
            File file = new File(configPath, name);
            if (file.exists() && file.isFile()) {
                try {
                    return file.toURI().toURL();
                } catch (MalformedURLException ignore) {
                }
            }
            return resourcer.findResource(name);
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name, ResourceFinder resourcer) throws IOException {
        // agent spi files
        boolean isolation = config.isIsolation(name);
        if (isolation) {
            return resourcer.findResources(name);
        }
        return null;
    }
}
