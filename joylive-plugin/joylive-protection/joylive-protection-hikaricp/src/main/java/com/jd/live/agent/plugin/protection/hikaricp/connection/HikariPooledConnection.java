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
package com.jd.live.agent.plugin.protection.hikaricp.connection;

import com.jd.live.agent.governance.db.jdbc.connection.DriverConnection;
import com.jd.live.agent.governance.db.jdbc.connection.PooledConnection;
import com.jd.live.agent.governance.util.network.ClusterRedirect;

import java.sql.Connection;
import java.util.function.Consumer;

/**
 * Pooled connection implementation using HikariCP.
 * Extends base pooled connection with Hikari-specific behaviors.
 */
public class HikariPooledConnection extends PooledConnection {

    private final Consumer<HikariPooledConnection> onClose;

    private final Object poolEntry;

    public HikariPooledConnection(Connection delegate,
                                  ClusterRedirect address,
                                  DriverConnection driver,
                                  Object poolEntry,
                                  Consumer<HikariPooledConnection> onClose) {
        super(delegate, address, driver);
        this.onClose = onClose;
        this.poolEntry = poolEntry;
    }

    @Override
    protected void doClose() {
        onClose.accept(this);
    }

    @Override
    protected void evictConnection() {
        driver.getDataSource().evict(this);
    }

    public Object getPoolEntry() {
        return poolEntry;
    }
}
