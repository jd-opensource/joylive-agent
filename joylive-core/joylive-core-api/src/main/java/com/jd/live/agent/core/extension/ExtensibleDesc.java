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
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * ExtensibleDesc is an interface that represents a description of an extensible component.
 * It provides methods to manage and access extensions of type T, associated with a specific name.
 * This interface extends the Function interface to allow the use of the apply method for retrieving extensions.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public interface ExtensibleDesc<T> extends Function<String, T> {

    /**
     * Retrieves the name of this extensible description.
     *
     * @return the name of this extensible component.
     */
    Name<T> getName();

    /**
     * Gets the default extension associated with this extensible description.
     *
     * @return the default extension.
     */
    T getExtension();

    /**
     * Retrieves an extension by its name.
     *
     * @param name the name of the extension to retrieve.
     * @return the extension associated with the given name, or null if not found.
     */
    T getExtension(String name);

    /**
     * Retrieves an extension by its names, where the first matching name is returned.
     *
     * @param name the names of the extensions to search for.
     * @return the first matching extension, or null if none match.
     */
    T getExtension(String... name);

    /**
     * Returns the number of extensions associated with this extensible description.
     *
     * @return the number of extensions.
     */
    int getSize();

    /**
     * Checks if there are no extensions associated with this extensible description.
     *
     * @return true if there are no extensions, false otherwise.
     */
    default boolean isEmpty() {
        return getSize() <= 0;
    }

    /**
     * Retrieves all extensions associated with this extensible description.
     *
     * @return a list of all extensions.
     */
    List<T> getExtensions();

    /**
     * Retrieves a map of all extensions, where the key is the name of the extension.
     *
     * @return a map of extension names to extension instances.
     */
    Map<String, T> getExtensionMap();

    /**
     * Retrieves a filtered and potentially reversed list of extensions based on the provided predicate.
     *
     * @param predicate the predicate to filter the extensions.
     * @param reverse   if true, the order of the extensions will be reversed.
     * @return an iterable of filtered and potentially reversed extensions.
     */
    Iterable<T> getExtensions(Predicate<T> predicate, boolean reverse);

    /**
     * Retrieves the names of all extensions associated with this extensible description.
     *
     * @return a list of extension names.
     */
    List<String> getExtensionNames();

    /**
     * Checks which of the provided names are associated with extensions.
     *
     * @param names the names to check.
     * @return a list of names that are associated with extensions.
     */
    List<String> contains(List<String> names);

    /**
     * Retrieves the description of an extension by its name.
     *
     * @param name the name of the extension.
     * @return the description of the extension, or null if not found.
     */
    ExtensionDesc<T> getExtensionDesc(String name);

    /**
     * Retrieves all descriptions of extensions associated with this extensible description.
     *
     * @return an iterable of all extension descriptions.
     */
    Iterable<ExtensionDesc<T>> getExtensionDescs();

    /**
     * Retrieves the descriptions of extensions with a specific name.
     *
     * @param name the name of the extensions to retrieve descriptions for.
     * @return an iterable of extension descriptions with the specified name.
     */
    Iterable<ExtensionDesc<T>> getExtensionDescs(String name);

    /**
     * Default implementation of the apply method from the Function interface.
     * Delegates to the getExtension method with the provided name.
     *
     * @param s the name of the extension to retrieve.
     * @return the extension associated with the given name.
     */
    @Override
    default T apply(String s) {
        return getExtension(s);
    }
}

