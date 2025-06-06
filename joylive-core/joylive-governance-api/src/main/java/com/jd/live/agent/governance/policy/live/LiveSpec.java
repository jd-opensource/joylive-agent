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
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.map.ListBuilder;
import com.jd.live.agent.core.util.map.MapBuilder;
import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import com.jd.live.agent.governance.policy.live.db.LiveDatabaseGroup;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

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

    @Getter
    @Setter
    private List<LiveDatabaseGroup> databaseGroups;

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

    private final transient Cache<String, LiveDatabase> databaseCache = new MapCache<>(new MapBuilder<String, LiveDatabase>() {
        @Override
        public Map<String, LiveDatabase> build() {
            Map<String, LiveDatabase> result = new HashMap<>();
            if (databaseGroups != null) {
                for (LiveDatabaseGroup databaseGroup : databaseGroups) {
                    List<LiveDatabase> databases = databaseGroup.getDatabases();
                    if (databases != null) {
                        for (LiveDatabase database : databases) {
                            Set<String> nodes = database.getNodes();
                            if (nodes != null && !nodes.isEmpty()) {
                                for (String node : nodes) {
                                    result.put(node, database);
                                    // for development environment
                                    addAlias(node, database, result);
                                }
                            }
                        }
                    }
                }
            }
            return result;
        }

        private void addAlias(String node, LiveDatabase database, Map<String, LiveDatabase> result) {
            URI uri = URI.parse(node);
            if (uri != null) {
                String host = uri.getHost();
                Integer port = uri.getPort();
                host = host == null ? null : host.toLowerCase();
                if (Ipv4.isLocalHost(host)) {
                    Ipv4.LOCAL_HOST.forEach(h -> result.put(URI.getAddress(h, port), database));
                }
            }
        }
    });

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

    public LiveDatabase getDatabase(String address) {
        return databaseCache.get(address);
    }

    public LiveDatabase getDatabase(String[] shards) {
        if (shards == null) {
            return null;
        }
        for (String shard : shards) {
            LiveDatabase database = getDatabase(shard);
            if (database != null) {
                return database;
            }
        }
        return null;
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
        if (databaseGroups != null) {
            databaseGroups.forEach(LiveDatabaseGroup::cache);
        }
        if (variables != null) {
            variables.forEach(LiveVariable::cache);
        }
        if (unitRules != null) {
            unitRules.forEach(UnitRule::cache);
        }
        // after databaseGroups cache, databaseCache is ready
        getDatabase("");
    }

    public static boolean isChanged(List<LiveDatabaseGroup> oldGroups, List<LiveDatabaseGroup> newGroups) {
        if (oldGroups == null || oldGroups.isEmpty()) {
            return newGroups != null && !newGroups.isEmpty();
        } else if (newGroups == null || newGroups.isEmpty()) {
            return true;
        } else if (oldGroups.size() != newGroups.size()) {
            return true;
        }
        Map<String, LiveDatabaseGroup> newGroupMap = new HashMap<>(newGroups.size());
        newGroups.forEach(newGroup -> newGroupMap.put(newGroup.getId(), newGroup));
        for (LiveDatabaseGroup oldGroup : oldGroups) {
            LiveDatabaseGroup newGroup = newGroupMap.get(oldGroup.getId());
            if (newGroup == null) {
                return true;
            }
            List<LiveDatabase> oldDatabases = oldGroup.getDatabases();
            List<LiveDatabase> newDatabases = newGroup.getDatabases();
            if (oldDatabases == null || oldDatabases.isEmpty()) {
                return newDatabases != null && !newDatabases.isEmpty();
            } else if (newDatabases == null || newDatabases.isEmpty()) {
                return true;
            } else if (oldDatabases.size() != newDatabases.size()) {
                return true;
            }
            Map<String, LiveDatabase> newDatabaseMap = new HashMap<>(newDatabases.size());
            newDatabases.forEach(newDatabase -> newDatabaseMap.put(newDatabase.getId(), newDatabase));
            for (LiveDatabase oldDatabase : oldDatabases) {
                LiveDatabase newDatabase = newDatabaseMap.get(oldDatabase.getId());
                if (newDatabase == null) {
                    return true;
                }
                if (!Objects.equals(oldDatabase.getAddresses(), newDatabase.getAddresses())
                        || oldDatabase.getRole() != newDatabase.getRole()
                        || oldDatabase.getAccessMode() != newDatabase.getAccessMode()) {
                    return true;
                }
            }
        }
        return false;
    }

}
