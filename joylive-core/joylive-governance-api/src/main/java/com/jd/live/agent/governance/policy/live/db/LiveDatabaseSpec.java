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
package com.jd.live.agent.governance.policy.live.db;

import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.map.MapBuilder;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Database specs synced from policy service with versioned groups.
 */
public class LiveDatabaseSpec implements LiveDatabaseSupervisor {

    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private long version;

    @Getter
    @Setter
    private List<LiveDatabaseGroup> groups;

    private final transient Cache<String, LiveDatabase> databaseCache = new MapCache<>(new MapBuilder<String, LiveDatabase>() {
        @Override
        public Map<String, LiveDatabase> build() {
            Map<String, LiveDatabase> result = new HashMap<>();
            if (groups != null) {
                for (LiveDatabaseGroup group : groups) {
                    List<LiveDatabase> databases = group.getDatabases();
                    if (databases != null) {
                        for (LiveDatabase database : databases) {
                            Set<String> nodes = database.getNodes();
                            if (nodes != null && !nodes.isEmpty()) {
                                for (String node : nodes) {
                                    result.put(node, database);
                                }
                            }
                        }
                    }
                }
            }
            return result;
        }
    });

    @Override
    public LiveDatabase getDatabase(String address) {
        return databaseCache.get(address);
    }

    @Override
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

    @Override
    public LiveDatabase getWriteDatabase(String... shards) {
        LiveDatabase database = getDatabase(shards);
        return database == null ? null : database.getWriteDatabase();
    }

    @Override
    public LiveDatabase getReadDatabase(String unit, String cell, String... shards) {
        LiveDatabase database = getDatabase(shards);
        return database == null ? null : database.getReadDatabase(unit, cell);
    }

    public void cache() {
        if (groups != null) {
            groups.forEach(LiveDatabaseGroup::cache);
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
