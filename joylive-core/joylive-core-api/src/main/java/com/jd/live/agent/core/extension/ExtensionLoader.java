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
package com.jd.live.agent.core.extension;

import java.util.List;

/**
 * The {@code ExtensionLoader} interface is responsible for loading extensions based on a given
 * extensible interface. It abstracts the process of locating and instantiating extensions,
 * allowing for a more modular and extensible application design.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public interface ExtensionLoader {

    /**
     * Loads extensions for a given extensible interface. This method is expected to find and
     * return a list of extension descriptors that are associated with the provided
     * extensible interface.
     *
     * @param <T>         the type of the extensible interface
     * @param extensible the extensible interface for which extensions are to be loaded
     * @return a list of extension descriptors that are instances of the extensible interface
     */
    <T> List<ExtensionDesc<T>> load(Class<T> extensible);

    /**
     * Retrieves the {@code ClassLoader} associated with this extension loader. The class loader
     * is used to load classes and resources for the extensions.
     *
     * @return the class loader used by this extension loader
     */
    ClassLoader getClassLoader();
}
