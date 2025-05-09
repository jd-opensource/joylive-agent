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

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * LiveSpace
 *
 * @since 1.0.0
 */
@Getter
public class LiveSpace {

    @Setter
    private String apiVersion;

    @Setter
    private String kind;

    @Setter
    private Map<String, String> metadata;

    @Setter
    private LiveSpec spec;

    private transient Unit localUnit;

    private transient Cell localCell;

    public LiveSpace() {
    }

    public LiveSpace(LiveSpec spec) {
        this.spec = spec;
    }

    public String getMeta(String key) {
        return metadata == null ? null : metadata.get(key);
    }

    public String getId() {
        String result = spec == null ? null : spec.getId();
        return result == null ? "" : result;
    }

    public List<Unit> getUnits() {
        return spec == null ? null : spec.getUnits();
    }

    public int getUnitSize() {
        List<Unit> units = getUnits();
        return units == null ? 0 : units.size();
    }

    public Unit getUnit(String code) {
        return spec == null ? null : spec.getUnit(code);
    }

    public Unit getCenter() {
        return spec == null ? null : spec.getCenter();
    }

    public boolean isCenter(String code) {
        if (code == null) {
            return false;
        }
        Unit center = getCenter();
        return code.equals(center == null ? null : center.getCode());
    }

    public LiveDomain getDomain(String host) {
        return spec == null ? null : spec.getDomain(host);
    }

    public LiveVariable getVariable(String name) {
        return spec == null ? null : spec.getVariable(name);
    }

    public UnitRule getUnitRule(String id) {
        return spec == null ? null : spec.getUnitRule(id);
    }

    public void locate(String unit, String cell) {
        localUnit = getUnit(unit);
        localCell = localUnit == null ? null : localUnit.getCell(cell);
    }

    public void cache() {
        if (spec != null) {
            spec.cache();
        }
    }
}
