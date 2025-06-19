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
package com.jd.live.agent.governance.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.db.DbUrlParser;
import com.jd.live.agent.governance.db.jdbc.connection.DriverConnection;
import com.jd.live.agent.governance.db.jdbc.connection.PooledConnection;
import com.jd.live.agent.governance.db.jdbc.context.DriverContext;
import com.jd.live.agent.governance.db.jdbc.datasource.LiveDataSource;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbstractJdbcConnectionInterceptor
 */
public abstract class AbstractJdbcConnectionInterceptor<T extends PooledConnection> extends AbstractDbConnectionInterceptor<T> {

    private static final Map<DataSource, LiveDataSource> DATASOURCE = new ConcurrentHashMap<>();

    public AbstractJdbcConnectionInterceptor(PolicySupplier policySupplier,
                                             Application application,
                                             GovernanceConfig governanceConfig,
                                             Publisher<DatabaseEvent> publisher,
                                             Timer timer,
                                             Map<String, DbUrlParser> parsers) {
        super(policySupplier, application, governanceConfig, publisher, timer, parsers);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        DataSource dataSource = getDataSource(ctx);
        if (dataSource == null) {
            return;
        }
        LiveDataSource description = DATASOURCE.computeIfAbsent(dataSource, this::build);
        if (description.validate()) {
            DriverContext.set(description);
        }
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Connection connection = mc.getResult();
        DriverConnection driver = unwrap(connection);
        if (driver != null) {
            mc.setResult(createConnection(() -> build(connection, driver.getAddress(), driver, mc.getTarget())));
        }
    }

    @Override
    public void onExit(ExecutableContext ctx) {
        DriverContext.remove();
    }

    @Override
    protected void redirectTo(T connection, ClusterAddress address) {
        // destroy connection from pool.
        try {
            connection.evict();
        } catch (SQLException ignored) {
        }
        ClusterRedirect.redirect(connection.getAddress().newAddress(address), consumer);
    }

    /**
     * Builds an object of type T using the provided connection and related components.
     *
     * @param connection the database connection to be used
     * @param address    the cluster redirect address (if applicable)
     * @param driver     the live driver connection instance
     * @param target     additional target object needed for building
     * @return the built object of type T
     */
    protected abstract T build(Connection connection, ClusterRedirect address, DriverConnection driver, Object target);

    /**
     * Unwraps a connection to get its LiveDriverConnection implementation.
     *
     * @param connection the connection to unwrap
     * @return the LiveDriverConnection instance, or null if unwrapping fails
     */
    protected DriverConnection unwrap(Connection connection) {
        try {
            return connection.unwrap(DriverConnection.class);
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Retrieves the DataSource for the given execution context.
     *
     * @param ctx the execution context
     * @return the corresponding DataSource instance
     */
    protected abstract DataSource getDataSource(ExecutableContext ctx);

    /**
     * Builds a LiveDataSource from the given DataSource.
     *
     * @param dataSource the DataSource to describe
     * @return constructed DataSourceDescription
     */
    protected abstract LiveDataSource build(DataSource dataSource);

}
