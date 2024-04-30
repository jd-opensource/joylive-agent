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
package com.jd.live.agent.governance.policy.db;

import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.map.ListBuilder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class DatabaseCluster {

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String host;

    @Getter
    @Setter
    private int port;

    @Getter
    @Setter
    private DatabasePolicy policy;

    @Getter
    @Setter
    private List<Database> databases;

    private transient String address;

    private final transient Cache<String, Database> databaseCache = new MapCache<>(new ListBuilder<>(() -> databases, Database::getName));

    public void addDatabase(Database database) {
        if (database != null) {
            if (databases == null) {
                databases = new ArrayList<>();
            }
            databases.add(database);
        }
    }

    public Database getDatabase(String name) {
        return databaseCache.get(name);
    }

    public DatabasePolicy getPolicy(String name) {
        Database database = getDatabase(name);
        return database == null ? policy : database.getPolicy();
    }

    public String getAddress() {
        if (address == null) {
            address = port <= 0 ? host : host + ":" + port;
        }
        return address;
    }

    protected void supplement() {
        if (databases != null) {
            databases.forEach(d -> d.supplement(policy));
        }
    }

    public void cache() {
        supplement();
        getDatabase("");
    }
}
