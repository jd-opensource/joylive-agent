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

import com.jd.live.agent.core.bootstrap.resource.BootResourcer;
import com.jd.live.agent.core.extension.annotation.Extension;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A class that implements the ResourceFinder interface by searching for resources on the classpath.
 */
@Extension(value = "ClassloaderBootResourcer", order = BootResourcer.ORDER_CLASSLOADER)
public class ClassloaderBootResourcer implements BootResourcer {

    @Override
    public InputStream getResource(String resource) throws IOException {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        URL url = contextClassLoader.getResource(resource);
        if (url == null) {
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            if (systemClassLoader != contextClassLoader) {
                url = systemClassLoader.getResource(resource);
            }
        }
        return url == null ? null : url.openStream();
    }
}
