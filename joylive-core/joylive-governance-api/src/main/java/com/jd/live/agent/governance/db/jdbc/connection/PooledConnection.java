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
package com.jd.live.agent.governance.db.jdbc.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Base class for pooled connection implementations.
 * Manages connection lifecycle including recycling and disposal.
 */
public abstract class PooledConnection extends AbstractConnection {

    protected final DriverConnection driver;

    public PooledConnection(Connection delegate, DriverConnection driver) {
        super(delegate, driver.getAddress());
        this.driver = driver;
    }

    /**
     * Forcefully evicts and closes this connection.
     * Performs full cleanup including driver resources.
     *
     * @throws SQLException if any close operation fails
     */
    public void evict() throws SQLException {
        try {
            closed = true;
            evictConnection();
            driver.close();
            close();
        } finally {
            doClose();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == driver) return true;
        if (!(o instanceof PooledConnection)) return false;
        PooledConnection that = (PooledConnection) o;
        return Objects.equals(driver, that.driver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(driver);
    }

    /**
     * Evicts this connection from the pool.
     */
    protected void evictConnection() {
        driver.getDataSource().evict(delegate);
    }

    /**
     * Performs final cleanup when connection closes
     */
    protected void doClose() {

    }
}
