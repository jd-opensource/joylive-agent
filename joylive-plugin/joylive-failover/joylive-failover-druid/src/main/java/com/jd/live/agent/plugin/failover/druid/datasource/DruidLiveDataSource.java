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
package com.jd.live.agent.plugin.failover.druid.datasource;

import com.alibaba.druid.pool.DruidAbstractDataSource;
import com.jd.live.agent.governance.db.DbUrl;
import com.jd.live.agent.governance.db.jdbc.datasource.LiveDataSource;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Druid-backed implementation of {@link LiveDataSource}.
 * Wraps a Druid connection pool with enhanced lifecycle management.
 */
public class DruidLiveDataSource implements LiveDataSource {

    private final DruidAbstractDataSource dataSource;

    private final DbUrl dbUrl;

    public DruidLiveDataSource(DruidAbstractDataSource dataSource, DbUrl dbUrl) {
        this.dataSource = dataSource;
        this.dbUrl = dbUrl;
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public String getPoolName() {
        return dataSource.getName();
    }

    @Override
    public DbUrl getUrl() {
        return dbUrl;
    }

    @Override
    public void evict(Connection connection) {
        dataSource.discardConnection(connection);
    }

    @Override
    public boolean validate() {
        return dbUrl != null && dbUrl.hasAddress();
    }
}
