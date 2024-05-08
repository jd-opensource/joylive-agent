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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AttributeAccessorSupport implements AttributeAccessor, Serializable {

    private Map<String, Object> attributes;

    public AttributeAccessorSupport() {
    }

    public void setAttribute(String key, Object value) {
        if (key != null && value != null) {
            if (attributes == null) {
                attributes = new LinkedHashMap<>();
            }
            attributes.put(key, value);
        }
    }

    public <T> T getAttribute(String key) {
        return key == null || attributes == null ? null : (T) attributes.get(key);
    }

    public <T> T removeAttribute(String key) {
        if (key != null && attributes != null) {
            return (T) attributes.remove(key);
        }
        return null;
    }

    public boolean hasAttribute(String key) {
        return this.attributes.containsKey(key);
    }

    public String[] attributeNames() {
        return this.attributes.keySet().toArray(new String[0]);
    }

    public void copyAttributesFrom(AttributeAccessor source) {
        String[] attributeNames = source.attributeNames();
        for (String name : attributeNames) {
            this.setAttribute(name, source.getAttribute(name));
        }
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof AttributeAccessorSupport)) {
            return false;
        } else {
            AttributeAccessorSupport that = (AttributeAccessorSupport) other;
            return this.attributes.equals(that.attributes);
        }
    }

    public int hashCode() {
        return this.attributes.hashCode();
    }
}