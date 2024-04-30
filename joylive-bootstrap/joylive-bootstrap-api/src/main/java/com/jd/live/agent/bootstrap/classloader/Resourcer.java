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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * Defines an interface for resource loading and class loading functionalities. This interface extends
 * {@link ResourceFinder} to provide additional capabilities for class and resource retrieval, allowing
 * implementations to load classes and resources in a flexible manner.
 */
public interface Resourcer extends ResourceFinder {

    /**
     * The identifier for the resourcer component.
     */
    String COMPONENT_RESOURCER = "resourcer";

    /**
     * The identifier for the core class loader component.
     */
    String COMPONENT_CLASSLOADER_CORE = "coreClassLoader";

    /**
     * The identifier for the core implementation class loader component.
     */
    String COMPONENT_CLASSLOADER_CORE_IMPL = "coreImplClassLoader";

    /**
     * Loads the class with the specified name.
     *
     * @param name The binary name of the class to load.
     * @return The resulting {@link Class} object.
     * @throws ClassNotFoundException If the class was not found.
     */
    Class<?> loadClass(String name) throws ClassNotFoundException;

    /**
     * Loads the class with the specified name, with an option to resolve it.
     *
     * @param name    The binary name of the class to load.
     * @param resolve If {@code true}, then resolve the class.
     * @return The resulting {@link Class} object.
     * @throws ClassNotFoundException If the class was not found.
     */
    Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException;

    /**
     * Loads the class with the specified name, with options to resolve it and provide a custom {@link CandidatorProvider}.
     *
     * @param name               The binary name of the class to load.
     * @param resolve            If {@code true}, then resolve the class.
     * @param candidatorProvider Provides a mechanism to influence the class loading process.
     * @return The resulting {@link Class} object.
     * @throws ClassNotFoundException If the class was not found.
     */
    Class<?> loadClass(String name, boolean resolve, CandidatorProvider candidatorProvider) throws ClassNotFoundException;

    /**
     * Retrieves a resource located by a specified path.
     *
     * @param path The path to the resource.
     * @return A {@link URL} object for reading the resource, or {@code null} if the resource could not be found.
     * @throws IOException If an I/O error occurs.
     */
    URL getResource(String path) throws IOException;

    /**
     * Retrieves all the resources located by a specified path.
     *
     * @param path The path to the resources.
     * @return An enumeration of {@link URL} objects for reading the resources.
     * @throws IOException If an I/O error occurs.
     */
    Enumeration<URL> getResources(String path) throws IOException;

    /**
     * Gets the type of the resourcer. This can be used to distinguish between different implementations
     * or strategies of resource and class loading.
     *
     * @return The {@link ResourcerType} of this resourcer.
     */
    ResourcerType getType();
}

