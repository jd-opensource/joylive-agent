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

import java.net.URL;

/**
 * Factory interface for creating class loader instances.
 */
public interface ClassLoaderFactory {

    /**
     * Creates a class loader with the given name and no URLs.
     *
     * @param name The name of the class loader to create.
     * @return A new class loader instance with the specified name.
     */
    default LiveClassLoader create(String name) {
        return create(name, new URL[0]);
    }

    /**
     * Creates a class loader with the given name and URLs.
     *
     * @param name The name of the class loader to create.
     * @param urls An array of URLs to be used for class loading.
     * @return A new class loader instance with the specified name and URLs.
     */
    LiveClassLoader create(String name, URL[] urls);
}

