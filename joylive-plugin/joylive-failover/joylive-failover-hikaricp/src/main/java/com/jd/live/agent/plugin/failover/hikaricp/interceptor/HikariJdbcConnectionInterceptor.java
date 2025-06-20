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
package com.jd.live.agent.plugin.failover.hikaricp.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.db.DbUrlParser;
import com.jd.live.agent.governance.db.jdbc.connection.DriverConnection;
import com.jd.live.agent.governance.db.jdbc.datasource.LiveDataSource;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.interceptor.AbstractJdbcConnectionInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.failover.hikaricp.connection.HikariPooledConnection;
import com.jd.live.agent.plugin.failover.hikaricp.datasource.HikariLiveDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.setValue;

/**
 * HikariCreateConnectionInterceptor
 */
public class HikariJdbcConnectionInterceptor extends AbstractJdbcConnectionInterceptor<HikariPooledConnection> {

    public HikariJdbcConnectionInterceptor(PolicySupplier policySupplier,
                                           Application application,
                                           GovernanceConfig governanceConfig,
                                           Publisher<DatabaseEvent> publisher,
                                           Timer timer,
                                           Map<String, DbUrlParser> parsers) {
        super(policySupplier, application, governanceConfig, publisher, timer, parsers);
    }

    @Override
    protected DataSource getDataSource(ExecutableContext ctx) {
        HikariConfig config = getQuietly(ctx.getTarget(), "config");
        if (config instanceof HikariDataSource) {
            return (HikariDataSource) config;
        }
        return null;
    }

    @Override
    protected LiveDataSource build(DataSource dataSource) {
        HikariDataSource hikari = ((HikariDataSource) dataSource);
        return new HikariLiveDataSource(hikari, DbUrlParser.parse(hikari.getJdbcUrl(), parsers::get));
    }

    @Override
    protected ConnectionUpdater getConnectionUpdater(MethodContext ctx) {
        return new HikariConnectionUpdater(ctx.getResult());
    }

    @Override
    protected HikariPooledConnection build(Connection connection, DriverConnection driver, MethodContext ctx) {
        return new HikariPooledConnection(connection, driver, ctx.getResult(), closer);
    }

    /**
     * HikariCP-specific implementation of ConnectionUpdater using reflection.
     * Manages connections through Hikari's internal pool entry object.
     */
    private static class HikariConnectionUpdater implements ConnectionUpdater {

        private final Object poolEntry;

        HikariConnectionUpdater(Object poolEntry) {
            this.poolEntry = poolEntry;
        }

        @Override
        public Connection getConnection() {
            return getQuietly(poolEntry, "connection");
        }

        @Override
        public void update(Connection connection) {
            setValue(poolEntry, "connection", connection);
        }
    }
}
