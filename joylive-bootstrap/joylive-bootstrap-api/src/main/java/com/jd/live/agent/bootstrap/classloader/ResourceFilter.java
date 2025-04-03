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
 * An interface extending {@link CandidatorProvider} to define filtering behavior
 * for class loading and resource retrieval.
 */
public interface ResourceFilter extends CandidatorProvider {

    /**
     * Determines whether a resource with the given name should be loaded by the parent class loader.
     *
     * @param name The name of the resource.
     * @return {@code true} if the resource should be loaded by the parent class loader; {@code false} otherwise.
     */
    boolean loadByParent(String name);

    /**
     * Determines whether a resource with the given name should be loaded by the this class loader.
     *
     * @param name The name of the resource.
     * @return {@code true} if the resource should be loaded by the this class loader; {@code false} otherwise.
     */
    boolean loadBySelf(String name);

    /**
     * Retrieves a resource identified by the given name using the provided {@link ResourceFinder}.
     *
     * @param name      The name of the resource to retrieve.
     * @param resourcer The {@link ResourceFinder} to use for finding the resource.
     * @return The URL to the resource, or {@code null} if the resource could not be found.
     */
    URL getResource(String name, ResourceFinder resourcer);

    /**
     * Retrieves all resources identified by the given name using the provided {@link ResourceFinder}.
     *
     * @param name      The name of the resources to retrieve.
     * @param resourcer The {@link ResourceFinder} to use for finding the resources.
     * @return An Enumeration of URLs to the resources.
     * @throws IOException If an I/O error occurs while retrieving the resources.
     */
    Enumeration<URL> getResources(String name, ResourceFinder resourcer) throws IOException;
}

