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

import com.jd.live.agent.core.parser.json.JsonAlias;
import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.map.ListBuilder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * LiveSpec
 *
 * @since 1.0.0
 */
public class LiveSpec {
    @JsonAlias("workspaceId")
    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private long version;

    @Getter
    @Setter
    private String tenantId;

    @Getter
    @Setter
    private List<Unit> units;

    @Getter
    @Setter
    private List<LiveDomain> domains;

    @Getter
    @Setter
    private List<UnitRule> unitRules;

    @Getter
    @Setter
    private List<LiveVariable> variables;

    @Getter
    @Setter
    private Set<String> topics;

    private final transient Cache<String, Unit> unitCache = new MapCache<>(new ListBuilder<>(() -> units, Unit::getCode));

    private final transient Cache<String, LiveDomain> domainCache = new MapCache<>(new ListBuilder<>(() -> domains, LiveDomain::getHost));

    private final transient Cache<String, LiveVariable> variableCache = new MapCache<>(new ListBuilder<>(() -> variables, LiveVariable::getName));

    private final transient Cache<String, UnitRule> unitRuleCache = new MapCache<>(new ListBuilder<>(() -> unitRules, rule -> {
        List<UnitRoute> unitRoutes = rule.getUnitRoutes();
        if (unitRoutes != null) {
            for (UnitRoute unitRoute : unitRoutes) {
                Unit unit = getUnit(unitRoute.getCode());
                if (unit != null) {
                    unitRoute.setUnit(unit);
                    List<CellRoute> cellRoutes = unitRoute.getCells();
                    if (cellRoutes != null) {
                        for (CellRoute cellRoute : cellRoutes) {
                            cellRoute.setCell(unit.getCell(cellRoute.getCode()));
                        }
                    } else if (unit.getCells() != null) {
                        // Fault tolerance for the absence of partition configuration under unit rules
                        cellRoutes = new ArrayList<>();
                        for (Cell cell : unit.getCells()) {
                            CellRoute cellRoute = new CellRoute();
                            cellRoute.setCode(cell.getCode());
                            cellRoute.setCell(cell);
                            cellRoute.setWeight(1);
                            cellRoutes.add(cellRoute);
                        }
                        unitRoute.setCells(cellRoutes);
                    }
                }
            }
        }
    }, UnitRule::getId));

    private final transient LazyObject<Unit> center = new LazyObject<>(() -> {
        for (Unit unit : units) {
            if (unit.getType() == UnitType.CENTER) {
                return unit;
            }
        }
        return null;
    });

    public LiveSpec() {
    }

    public LiveSpec(String id) {
        this.id = id;
    }

    public LiveSpec(String id, String code, String name, String tenantId) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.tenantId = tenantId;
    }

    public Unit getUnit(String code) {
        return unitCache.get(code);
    }

    public LiveDomain getDomain(String host) {
        return domainCache.get(host);
    }

    public LiveVariable getVariable(String name) {
        return variableCache.get(name);
    }

    public UnitRule getUnitRule(String id) {
        return id == null ? null : unitRuleCache.get(id);
    }

    public boolean withTopic(String topic) {
        return topic != null && topics != null && topics.contains(topic);
    }

    public Unit getCenter() {
        return center.get();
    }

    public void cache() {
        getUnit("");
        getDomain("");
        getVariable("");
        getUnitRule("");
        getCenter();
        if (units != null) {
            units.forEach(Unit::cache);
        }
        if (domains != null) {
            domains.forEach(LiveDomain::cache);
        }
        if (variables != null) {
            variables.forEach(LiveVariable::cache);
        }
        if (unitRules != null) {
            unitRules.forEach(UnitRule::cache);
        }
    }

}
