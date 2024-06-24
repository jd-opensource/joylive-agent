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
 * Manages extensions, providing capabilities to retrieve and load extensions dynamically.
 * This interface defines methods for managing the lifecycle of extensions,
 * including retrieval, loading, and removing extensions or their descriptions.
 * Additionally, it supports adding and removing listeners for extension events.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public interface ExtensionManager {
    /**
     * The name of the component for extension management.
     */
    String COMPONENT_EXTENSION_MANAGER = "extensionManager";

    /**
     * Retrieves an extension by its type and name.
     *
     * @param <T>  The type of the extension.
     * @param type The type of the extension.
     * @param name The name of the extension.
     * @return The retrieved extension.
     */
    <T> T getExtension(String type, String name);

    /**
     * Retrieves an extension by its class and name.
     *
     * @param <T>        The type of the extension.
     * @param extensible The class of the extension.
     * @param name       The name of the extension.
     * @return The retrieved extension.
     */
    <T> T getExtension(Class<T> extensible, String name);

    /**
     * Retrieves or loads an extension by its class.
     *
     * @param <T>        The type of the extension.
     * @param extensible The class of the extension.
     * @return The retrieved or loaded extension.
     */
    <T> T getOrLoadExtension(Class<T> extensible);

    /**
     * Retrieves or loads an extension by its class using a specified class loader.
     *
     * @param <T>        The type of the extension.
     * @param extensible  The class of the extension.
     * @param classLoader The class loader to use.
     * @return The retrieved or loaded extension.
     */
    <T> T getOrLoadExtension(Class<T> extensible, ClassLoader classLoader);

    /**
     * Retrieves or loads an extension by its class and name.
     *
     * @param <T>        The type of the extension.
     * @param extensible The class of the extension.
     * @param name       The name of the extension.
     * @return The retrieved or loaded extension.
     */
    <T> T getOrLoadExtension(Class<T> extensible, String name);

    /**
     * Retrieves or loads an extension by its class and name using a specified class loader.
     *
     * @param <T>        The type of the extension.
     * @param extensible  The class of the extension.
     * @param name        The name of the extension.
     * @param classLoader The class loader to use.
     * @return The retrieved or loaded extension.
     */
    <T> T getOrLoadExtension(Class<T> extensible, String name, ClassLoader classLoader);

    /**
     * Retrieves or loads the description of an extensible by its class.
     *
     * @param <T>        The type of the extension.
     * @param extensible The class of the extensible.
     * @return The description of the retrieved or loaded extensible.
     */
    <T> ExtensibleDesc<T> getOrLoadExtensible(Class<T> extensible);

    /**
     * Retrieves or loads the description of an extensible by its class using a specified class loader.
     *
     * @param <T>        The type of the extension.
     * @param extensible  The class of the extensible.
     * @param classLoader The class loader to use.
     * @return The description of the retrieved or loaded extensible.
     */
    <T> ExtensibleDesc<T> getOrLoadExtensible(Class<T> extensible, ClassLoader classLoader);

    /**
     * Retrieves or loads the description of an extensible by its class using a specified extension loader.
     *
     * @param <T>        The type of the extension.
     * @param extensible      The class of the extensible.
     * @param extensionLoader The extension loader to use.
     * @return The description of the retrieved or loaded extensible.
     */
    <T> ExtensibleDesc<T> getOrLoadExtensible(Class<T> extensible, ExtensionLoader extensionLoader);

    /**
     * Loads the description of an extensible by its class.
     *
     * @param <T>        The type of the extension.
     * @param extensible The class of the extensible.
     * @return The loaded description of the extensible.
     */
    <T> ExtensibleDesc<T> loadExtensible(Class<T> extensible);

    /**
     * Loads the description of an extensible by its class using a specified class loader.
     *
     * @param <T>        The type of the extension.
     * @param extensible  The class of the extensible.
     * @param classLoader The class loader to use.
     * @return The loaded description of the extensible.
     */
    <T> ExtensibleDesc<T> loadExtensible(Class<T> extensible, ClassLoader classLoader);

    /**
     * Loads the description of an extensible by its class using a specified extension loader.
     *
     * @param <T>        The type of the extension.
     * @param extensible      The class of the extensible.
     * @param extensionLoader The extension loader to use.
     * @return The loaded description of the extensible.
     */
    <T> ExtensibleDesc<T> loadExtensible(Class<T> extensible, ExtensionLoader extensionLoader);

    /**
     * Builds an extensible loader for an extensible class using a specified class loader.
     *
     * @param <T>        The type of the extension.
     * @param extensible  The class of the extensible.
     * @param classLoader The class loader to use.
     * @return The built extensible loader.
     */
    <T> ExtensibleLoader<T> build(Class<T> extensible, ClassLoader classLoader);

    /**
     * Removes all extensions associated with a specified class loader.
     *
     * @param classLoader The class loader to remove.
     */
    void remove(ClassLoader classLoader);

    /**
     * Removes all extensions of a specified class.
     *
     * @param extensible The class of the extensions to remove.
     */
    void remove(Class<?> extensible);

    /**
     * Adds a listener for extension events.
     *
     * @param listener The listener to add.
     */
    void addListener(ExtensionListener listener);

    /**
     * Removes a listener for extension events.
     *
     * @param listener The listener to remove.
     */
    void removeListener(ExtensionListener listener);
}

