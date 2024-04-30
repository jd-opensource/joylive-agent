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

/**
 * The {@code ExtensibleLoader} interface provides a contract for loading and managing extensions.
 * It allows for the retrieval of extensions by name, loading or reloading of extensions, and
 * obtaining descriptions of extensible components. Additionally, it ensures that resources
 * can be properly closed after use by implementing the {@code AutoCloseable} interface.
 *
 * @param <T> the type of the extensible component that this loader is designed to handle
 */
public interface ExtensibleLoader<T> extends AutoCloseable {

    /**
     * Retrieves the extension with the specified name. If the extension is not already loaded,
     * it may be loaded automatically.
     *
     * @param name the name of the extension to retrieve
     * @return the extension with the given name, or {@code null} if no such extension exists
     * @throws Exception if an error occurs during the retrieval or loading of the extension
     */
    T getExtension(String name) throws Exception;

    /**
     * Gets the current extension, or loads it if it's not already loaded.
     *
     * @return the current extension instance
     * @throws Exception if an error occurs during the loading of the extension
     */
    T getOrLoadExtension() throws Exception;

    /**
     * Gets a description of the extensible component, loading it if necessary.
     *
     * @return a description of the extensible component
     * @throws Exception if an error occurs during the loading of the extensible component
     */
    ExtensibleDesc<T> getOrLoadExtensible() throws Exception;

    /**
     * Loads the extensible component and returns a description of it.
     *
     * @return a description of the loaded extensible component
     * @throws Exception if an error occurs during the loading process
     */
    ExtensibleDesc<T> loadExtensible() throws Exception;

    /**
     * Gets the {@code ClassLoader} that is used by this loader for class loading operations.
     *
     * @return the associated {@code ClassLoader}
     */
    ClassLoader getClassLoader();

    /**
     * Gets the {@code Class} object that represents the extensible component type.
     *
     * @return the {@code Class} object for the extensible component type
     */
    Class<T> getExtensible();

    /**
     * Closes this loader and releases any resources associated with it. This method is invoked
     * automatically when the try-with-resources statement is exited and if the loader is
     * created within the try block.
     *
     * @throws Exception if an error occurs when closing the loader
     */
    void close() throws Exception;
}

