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
package com.jd.live.agent.plugin.protection.jdbc.datasource;

import com.jd.live.agent.governance.db.DbUrl;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Represents a managed database connection pool with lifecycle controls.
 * Provides access to the underlying data source and its metadata.
 */
public interface LiveDataSource {

    /**
     * Gets the underlying data source instance.
     *
     * @return the wrapped DataSource implementation
     */
    DataSource getDataSource();

    /**
     * Gets the name identifying this connection pool.
     *
     * @return the pool's configured name
     */
    String getPoolName();

    /**
     * Gets the database URL configuration.
     *
     * @return the parsed database URL information
     */
    DbUrl getUrl();

    /**
     * Forcefully discards a connection (removes from pool).
     *
     * @param connection the connection to invalidate
     */
    void discard(Connection connection);

    /**
     * Checks if the pool is currently valid and operational.
     *
     * @return true if the pool is healthy, false if failed
     */
    boolean validate();
}
