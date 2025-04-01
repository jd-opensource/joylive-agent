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
package com.jd.live.agent.core.bootstrap.resource.loader;

import com.jd.live.agent.core.bootstrap.resource.BootResource;
import com.jd.live.agent.core.bootstrap.resource.BootResourceLoader;
import com.jd.live.agent.core.bootstrap.resource.InputStreamResource;
import com.jd.live.agent.core.extension.annotation.Extension;

import java.io.IOException;
import java.net.URL;

import static com.jd.live.agent.core.bootstrap.resource.BootResource.SCHEMA_CLASSPATH;
import static com.jd.live.agent.core.util.StringUtils.concat;

/**
 * A class that implements the ResourceFinder interface by searching for resources on the classpath.
 */
@Extension(value = "ClasspathBootResourceLoader", order = BootResourceLoader.ORDER_CLASSPATH)
public class ClasspathBootResourceLoader implements BootResourceLoader {

    @Override
    public InputStreamResource getResource(BootResource resource) throws IOException {
        String[] paths = resource.withPath()
                ? new String[]{concat(resource.getPath(), resource.getPath(), "/")}
                : new String[]{resource.getName(), "config/" + resource.getName(), "BOOT-INF/classes/" + resource.getName(), "BOOT-INF/classes/config/" + resource.getName()};
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
        for (String path : paths) {
            URL url = contextLoader.getResource(path);
            if (url == null) {
                if (systemLoader != contextLoader) {
                    url = systemLoader.getResource(path);
                }
            }
            if (url != null) {
                return new InputStreamResource(url.openStream(), path);
            }
        }
        return null;
    }

    @Override
    public boolean support(String schema) {
        return schema == null || schema.isEmpty() || SCHEMA_CLASSPATH.equalsIgnoreCase(schema);
    }
}
