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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Courier implements Carrier {

    protected Map<String, Cargo> tags;

    protected Map<String, Object> attributes;

    @Override
    public Collection<Cargo> getCargos() {
        return tags == null ? null : tags.values();
    }

    @Override
    public Cargo getCargo(String key) {
        return tags == null ? null : tags.get(key);
    }

    @Override
    public void addCargo(Cargo cargo) {
        if (cargo != null) {
            String name = cargo.getKey();
            if (name != null && !name.isEmpty()) {
                if (tags == null) {
                    tags = new HashMap<>();
                }
                Cargo old = tags.putIfAbsent(cargo.getKey(), cargo);
                if (old != null && old != cargo) {
                    cargo.add(cargo.getValues());
                }
            }
        }
    }

    @Override
    public void addCargo(String key, String value) {
        if (key != null && !key.isEmpty()) {
            if (tags == null) {
                tags = new HashMap<>();
            }
            tags.computeIfAbsent(key, Cargo::new).add(value);
        }
    }

    @Override
    public void setCargo(String key, String value) {
        if (key != null && !key.isEmpty()) {
            if (tags == null) {
                tags = new HashMap<>();
            }
            tags.put(key, new Cargo(key, value));
        }
    }

    @Override
    public void removeCargo(String key) {
        if (key != null && !key.isEmpty() && tags != null) {
            tags.remove(key);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return key == null || attributes == null ? null : (T) attributes.get(key);
    }

    @Override
    public void setAttribute(String key, Object value) {
        if (key != null && value != null) {
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            attributes.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T removeAttribute(String key) {
        if (key != null && attributes != null) {
            return (T) attributes.remove(key);
        }
        return null;
    }
}
