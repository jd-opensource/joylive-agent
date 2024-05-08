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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Provides a skeletal implementation of the {@link Attributes} interface to minimize the effort required to implement this interface.
 * <p>
 * This abstract class implements the {@code copy} and {@code forEach} methods of the {@code Attributes} interface, relying on the concrete
 * class to implement the methods for accessing and modifying the attributes. The {@code copy} method uses the {@code forEach} method
 * from the provided {@code Attributes} instance to copy all attributes to the current instance. Concrete implementations of this class
 * must implement the {@code getAttribute}, {@code setAttribute}, {@code removeAttribute}, and {@code hasAttribute} methods.
 * </p>
 */
public abstract class AbstractAttributes implements Attributes {

    private Map<String, Object> attributes;

    public AbstractAttributes() {
    }

    @Override
    public void setAttribute(String key, Object value) {
        if (key != null && value != null) {
            if (attributes == null) {
                attributes = new LinkedHashMap<>();
            }
            attributes.put(key, value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return key == null || attributes == null ? null : (T) attributes.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T removeAttribute(String key) {
        if (key != null && attributes != null) {
            return (T) attributes.remove(key);
        }
        return null;
    }

    @Override
    public boolean hasAttribute(String key) {
        return key != null && attributes.containsKey(key);
    }

    @Override
    public void attributes(BiConsumer<String, Object> consumer) {
        if (attributes != null && consumer != null) {
            attributes.forEach(consumer);
        }
    }
}