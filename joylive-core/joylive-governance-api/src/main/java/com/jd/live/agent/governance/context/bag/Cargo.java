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
package com.jd.live.agent.governance.context.bag;

import com.jd.live.agent.core.util.tag.Tag;

import java.util.Collection;
import java.util.Enumeration;

/**
 * Represents a cargo, an entity for carrying data within a request, extending the functionality of a {@link Tag}.
 * <p>
 * Cargo objects are used to encapsulate key-value data pairs or key-values collections, facilitating the transport and management
 * of data throughout the lifecycle of a request. This class provides constructors and methods for handling both single and multiple
 * values associated with a key.
 * </p>
 */
public class Cargo extends Tag {
    /**
     * Constructs a cargo with a specified key and no values.
     *
     * @param key The key associated with the cargo.
     */
    public Cargo(String key) {
        super(key);
    }

    /**
     * Constructs a cargo with a specified key and a single value.
     *
     * @param key   The key associated with the cargo.
     * @param value The value associated with the key.
     */
    public Cargo(String key, String value) {
        super(key, value);
    }

    /**
     * Constructs a cargo with a specified key and a collection of values.
     *
     * @param key    The key associated with the cargo.
     * @param values The collection of values associated with the key.
     */
    public Cargo(String key, Collection<String> values) {
        super(key, values);
    }

    /**
     * Constructs a cargo with a specified key and a collection of values.
     *
     * @param key      The key associated with the cargo.
     * @param values   The collection of values associated with the key.
     * @param zeroCopy A flag indicating whether to perform a zero-copy operation.
     */
    public Cargo(String key, Collection<String> values, boolean zeroCopy) {
        super(key, values, zeroCopy);
    }

    /**
     * Constructs a cargo with a specified key and an enumeration of values.
     *
     * @param key    The key associated with the cargo.
     * @param values The enumeration of values associated with the key.
     */
    public Cargo(String key, Enumeration<String> values) {
        super(key, values);
    }

    /**
     * Adds a single value to the cargo.
     *
     * @param value The value to add.
     */
    public void add(String value) {
        super.add(value);
    }

    /**
     * Adds a collection of values to the cargo.
     *
     * @param items The collection of values to add.
     */
    public void add(Collection<String> items) {
        super.add(items);
    }

    /**
     * Adds an enumeration of values to the cargo.
     *
     * @param items The enumeration of values to add.
     */
    public void add(Enumeration<String> items) {
        super.add(items);
    }

    @Override
    public String toString() {
        return "Cargo{" +
                "key='" + key + '\'' +
                ", values='" + values + '\'' +
                '}';
    }
}

