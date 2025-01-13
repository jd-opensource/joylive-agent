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
package com.jd.live.agent.core.util.tag;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

/**
 * The {@code Tag} class represents a label with a key and a list of values.
 * This class implements the {@code Label} interface and is serializable, allowing it to be used across different contexts
 * where labels are needed, such as in tagging resources for identification or categorization.
 * <p>
 * The class provides constructors for creating tags with single or multiple values and supports adding new values to
 * the tag.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class Tag implements Label, Serializable {

    protected String key;

    protected List<String> values;

    protected Tag() {
    }

    /**
     * Constructs a {@code Tag} with the specified key.
     *
     * @param key The key of the tag.
     */
    public Tag(String key) {
        this.key = key;
    }

    /**
     * Constructs a {@code Tag} with the specified key and single value.
     *
     * @param key   The key of the tag.
     * @param value The value associated with the tag key.
     */
    public Tag(String key, String value) {
        this.key = key;
        add(value);
    }

    /**
     * Constructs a {@code Tag} with the specified key and a collection of values.
     *
     * @param key    The key of the tag.
     * @param values The collection of values associated with the tag key.
     */
    public Tag(String key, Collection<String> values) {
        this.key = key;
        add(values);
    }

    /**
     * Constructs a {@code Tag} with the specified key and a collection of values.
     *
     * @param key      The key of the tag.
     * @param values   The collection of values associated with the tag key.
     * @param zeroCopy A flag indicating whether to perform a zero-copy operation.
     */
    public Tag(String key, Collection<String> values, boolean zeroCopy) {
        this.key = key;
        add(values, zeroCopy);
    }

    /**
     * Constructs a {@code Tag} with the specified key and an enumeration of values.
     *
     * @param key    The key of the tag.
     * @param values The enumeration of values associated with the tag key.
     */
    public Tag(String key, Enumeration<String> values) {
        this.key = key;
        add(values);
    }

    @Override
    public String getKey() {
        return key;
    }

    protected void setKey(String key) {
        this.key = key;
    }

    @Override
    public List<String> getValues() {
        return values;
    }

    protected void setValues(List<String> values) {
        this.values = values;
    }

    @Override
    public String getFirstValue() {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    @Override
    public String getValue() {
        return Label.join(values);
    }

    /**
     * Adds a single value to the list of values associated with the tag.
     * If the list does not exist, it is initialized. The value is added only if it is not null and not already present in the list.
     *
     * @param value The value to be added to the list of values.
     */
    protected void add(String value) {
        if (values == null) {
            values = new ArrayList<>(1);
            if (value != null) {
                values.add(value);
            }
        } else if (value != null && !values.contains(value)) {
            values.add(value);
        }
    }

    /**
     * Adds a collection of values to the list of values associated with the tag.
     * This method ensures that only unique, non-null values are added. If the input collection is the same as the existing values list,
     * the method returns immediately to avoid infinite recursion. If the values list does not exist, it is initialized.
     *
     * @param items The collection of values to be added.
     */
    protected void add(Collection<String> items) {
        add(items, false);
    }

    /**
     * Adds a collection of values to the list of values associated with the tag.
     * This method ensures that only unique, non-null values are added. If the input collection is the same as the existing values list,
     * the method returns immediately to avoid infinite recursion. If the values list does not exist, it is initialized.
     *
     * @param items    The collection of values to be added.
     * @param zeroCopy A flag indicating whether to perform a zero-copy operation.
     */
    protected void add(Collection<String> items, boolean zeroCopy) {
        int size = items == null ? 0 : items.size();
        // call multi times in one thread.
        if (size == 0 || values == items) {
            return;
        }
        if (values == null) {
            values = zeroCopy && items instanceof ArrayList
                    ? (ArrayList<String>) items
                    : new ArrayList<>(items);
        } else {
            for (String v : items) {
                if (v != null && !values.contains(v)) {
                    values.add(v);
                }
            }
        }
    }

    /**
     * Adds an enumeration of values to the list of values associated with the tag.
     * This method converts the enumeration to a list and then calls {@code add(Collection<String> items)}
     * to add each value, ensuring that only unique, non-null values are added.
     *
     * @param items The enumeration of values to be added.
     */
    protected void add(Enumeration<String> items) {
        if (values == items) {
            return;
        }
        List<String> list = null;
        if (items != null) {
            list = new ArrayList<>(1);
            while (items.hasMoreElements()) {
                list.add(items.nextElement());
            }
        }
        add(list);
    }

    @Override
    public String toString() {
        return "Tag{" +
                "key='" + key + '\'' +
                ", values='" + values + '\'' +
                '}';
    }

}
