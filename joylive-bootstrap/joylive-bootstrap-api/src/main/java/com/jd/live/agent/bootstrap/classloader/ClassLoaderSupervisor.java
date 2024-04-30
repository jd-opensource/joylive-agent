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

/**
 * Supervisor interface that extends {@link ClassLoaderFactory} to add
 * functionality for removing and retrieving class loaders by name.
 */
public interface ClassLoaderSupervisor extends ClassLoaderFactory {

    /**
     * Removes a class loader identified by the given name.
     *
     * @param name The name of the class loader to remove.
     * @return The removed class loader, or {@code null} if no class loader with the given name existed.
     */
    ClassLoader remove(String name);

    /**
     * Retrieves a class loader identified by the given name.
     *
     * @param name The name of the class loader to retrieve.
     * @return The class loader with the specified name, or {@code null} if no such class loader exists.
     */
    ClassLoader get(String name);
}

