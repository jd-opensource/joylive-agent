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
package com.jd.live.agent.governance.policy.live;

import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.map.ListBuilder;
import com.jd.live.agent.governance.policy.AccessMode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

public class Unit implements Place {

    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    private String decorator;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private UnitType type;

    @Getter
    @Setter
    private AccessMode accessMode;

    @Getter
    @Setter
    private Map<String, String> labels;

    @Getter
    @Setter
    private List<Cell> cells;

    private final transient LazyObject<String> decoratorCache = new LazyObject<>(() -> {
        String result = decorator == null || decorator.isEmpty() ? code : decorator;
        return result == null ? "" : result.replace('_', '-').toLowerCase();
    });

    private final transient Cache<String, Cell> cellCache = new MapCache<>(new ListBuilder<>(() -> cells, Cell::getCode));

    @Override
    public FaultType getPlaceType() {
        return FaultType.UNIT;
    }

    public String getLabel(String key) {
        return labels == null ? null : labels.get(key);
    }

    public Cell getCell(String code) {
        return cellCache.get(code);
    }

    public void cache() {
        getCell("");
    }

    public String decorator() {
        return decoratorCache.get();
    }
}
