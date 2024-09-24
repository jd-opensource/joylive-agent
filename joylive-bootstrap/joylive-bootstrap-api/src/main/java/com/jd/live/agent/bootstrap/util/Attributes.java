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
package com.jd.live.agent.bootstrap.util;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Provides a contract for setting and getting attributes generically.
 * This interface allows for the storage and retrieval of attributes.
 */
public interface Attributes {

    /**
     * Retrieves an attribute by key.
     *
     * @param key The key of the attribute to retrieve.
     * @param <T> The type of the attribute value.
     * @return The value of the attribute, or null if not found.
     */
    <T> T getAttribute(String key);

    /**
     * Retrieves an attribute by key, or computes and stores a new attribute value if not found.
     *
     * @param key      The key of the attribute to retrieve.
     * @param function A function that computes a new attribute value if the attribute is not found.
     * @param <T>      The type of the attribute value.
     * @return The value of the attribute, or the computed value if not found.
     */
    <T> T getAttributeIfAbsent(String key, Function<String, T> function);

    /**
     * Sets or replaces an attribute with the specified key and value.
     *
     * @param key   The key of the attribute.
     * @param value The value of the attribute.
     */
    void setAttribute(String key, Object value);

    /**
     * Removes an attribute by its key.
     *
     * @param key The key of the attribute to remove.
     * @return The removed attribute, or null if the attribute was not found.
     */
    <T> T removeAttribute(String key);

    /**
     * Checks if an attribute with the specified key exists.
     * <p>This method is used to determine whether an attribute is associated with the given key.</p>
     *
     * @param key The key of the attribute to check. Cannot be {@code null}.
     * @return {@code true} if an attribute with the specified key exists; {@code false} otherwise.
     */
    boolean hasAttribute(String key);

    /**
     * Performs the given action for each attribute in this instance until all attributes
     * have been processed or the action throws an exception. Actions are performed in
     * the order of attribute insertion when possible.
     *
     * @param consumer The action to be performed for each attribute
     */
    void attributes(BiConsumer<String, Object> consumer);

    /**
     * Copies all source from the provided Attributes instance into this one.
     * If an attribute with the same key already exists in this instance, its value
     * is replaced with the value from the provided Attributes instance.
     *
     * @param source the Attributes instance from which to copy source
     */
    default void copyAttribute(Attributes source) {
        if (source != null) {
            source.attributes(this::setAttribute);
        }
    }
}