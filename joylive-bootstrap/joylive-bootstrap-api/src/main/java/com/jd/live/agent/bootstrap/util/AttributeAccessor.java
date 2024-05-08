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

public interface AttributeAccessor {

    /**
     * Retrieves an attribute by key.
     *
     * @param key The key of the attribute to retrieve.
     * @return The value of the attribute, or null if not found.
     */
    <T> T getAttribute(String key);

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

    boolean hasAttribute(String key);

    String[] attributeNames();
}