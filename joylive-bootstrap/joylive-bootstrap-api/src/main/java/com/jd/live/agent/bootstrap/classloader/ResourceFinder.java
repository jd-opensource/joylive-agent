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
 * Interface for finding resources by path.
 */
public interface ResourceFinder {

    /**
     * Finds a resource with the given path.
     *
     * @param path the resource path
     * @return the resource URL, or null if not found
     */
    URL findResource(String path);

    /**
     * Finds all resources with the given path.
     *
     * @param path the resource path
     * @return an enumeration of resource URLs
     * @throws IOException if an I/O error occurs
     */
    Enumeration<URL> findResources(String path) throws IOException;
}
