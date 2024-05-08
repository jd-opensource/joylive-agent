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

import com.jd.live.agent.bootstrap.util.AttributeAccessorSupport;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Courier extends AttributeAccessorSupport implements Carrier {

    protected Map<String, Cargo> tags;

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

}
