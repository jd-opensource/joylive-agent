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
package com.jd.live.agent.core.util.option;

import com.jd.live.agent.core.util.type.ValuePath;
import com.jd.live.agent.core.util.type.ValuePath.MapPath;

import java.util.HashMap;
import java.util.Map;

import static com.jd.live.agent.core.util.StringUtils.CHAR_DOT;
import static com.jd.live.agent.core.util.StringUtils.split;

public class CascadeOption extends AbstractOption {

    protected Map<String, Object> map;

    public CascadeOption(Map<String, Object> map) {
        this.map = map;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getObject(String key) {
        if (key == null || map == null) {
            return null;
        }
        ValuePath path = new MapPath(key, o -> o instanceof Map);
        return (T) path.get(map);
    }

    @SuppressWarnings("unchecked")
    public void put(String key, Object value) {
        if (key == null || value == null) {
            return;
        } else if (map == null) {
            map = new HashMap<>();
        }
        String[] keys = split(key, CHAR_DOT);
        Map<String, Object> parent = map;
        Object next;
        for (int i = 0; i < keys.length - 1; i++) {
            next = parent.get(keys[i]);
            if (next == null) {
                next = new HashMap<String, Object>();
                parent.put(keys[i], next);
                parent = (Map<String, Object>) next;
            } else if (next instanceof Map) {
                parent = (Map<String, Object>) next;
            } else {
                // none cascade
                map.put(key, value);
                return;
            }
        }
        parent.put(keys[keys.length - 1], value);
    }
}
