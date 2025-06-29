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

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class LiveDatabaseGroup {
    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String type;

    @Getter
    @Setter
    private String description;

    @Getter
    @Setter
    private List<LiveDatabase> databases;

    public LiveDatabase getWriteDatabase(LiveDatabase database) {
        if (database != null && database.getRole() == LiveDatabaseRole.MASTER) {
            return database;
        }
        for (LiveDatabase db : databases) {
            if (db.getRole() == LiveDatabaseRole.MASTER) {
                return db;
            }
        }
        return null;
    }

    public LiveDatabase getReadDatabase(LiveDatabase database, String unit, String cell) {
        if (database != null && database.getAccessMode().isReadable()) {
            return database;
        }
        int priority;
        int max = -1;
        LiveDatabase result = null;
        for (LiveDatabase db : databases) {
            if (db.getAccessMode().isReadable()) {
                priority = 100;
                priority += db.isLocation(unit, cell) ? 10 : 0;
                priority += db.getRole() == LiveDatabaseRole.MASTER ? 1 : 0;
                if (priority > max) {
                    max = priority;
                    result = db;
                }
            }
        }
        return result;
    }

    public void cache() {
        if (databases != null) {
            databases.forEach(d -> {
                d.cache();
                d.setGroup(this);
            });
        }
    }

}
