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
import com.jd.live.agent.governance.db.jdbc.connection.DriverConnection;
import com.jd.live.agent.governance.db.jdbc.connection.PooledConnection;
import com.jd.live.agent.governance.db.jdbc.context.DriverContext;
import com.jd.live.agent.governance.db.jdbc.datasource.LiveDataSource;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.invoke.InvocationContext;
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

    public AbstractJdbcConnectionInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        DataSource dataSource = getDataSource(ctx);
        if (dataSource == null) {
            return;
        }
        LiveDataSource ds = DATASOURCE.computeIfAbsent(dataSource, this::build);
        if (ds.validate()) {
            DriverContext.set(ds);
        }
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        ConnectionUpdater updater = getConnectionUpdater(mc);
        Connection connection = updater.getConnection();
        DriverConnection driver = unwrap(connection);
        if (driver != null) {
            T newConnection = createConnection(() -> build(connection, driver, mc));
            updater.update(newConnection);
            // Avoid missing events caused by synchronous changes
            ClusterRedirect redirect = driver.getAddress();
            DbCandidate candidate = getCandidate(redirect, PRIMARY_ADDRESS_RESOLVER);
            if (isChanged(redirect.getNewAddress(), candidate)) {
                publisher.offer(new DatabaseEvent(this));
            }
        }
    }

    @Override
    public void onExit(ExecutableContext ctx) {
        DriverContext.remove();
    }

    /**
     * Creates a new connection updater for the given method context.
     *
     * @param ctx the method execution context to wrap
     * @return new ConnectionUpdater instance
     */
    protected ConnectionUpdater getConnectionUpdater(MethodContext ctx) {
        return new DefaultConnectionUpdater(ctx);
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
     * Creates a new instance of type T using the provided components.
     * @param connection raw database connection
     * @param driver     connection driver handle
     * @param ctx        method invocation context
     * @return newly constructed instance
     */
    protected abstract T build(Connection connection, DriverConnection driver, MethodContext ctx);

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

    /**
     * Provides access to update a connection in current execution context.
     */
    protected interface ConnectionUpdater {
        /**
         * @return current connection instance
         */
        Connection getConnection();

        /**
         * @param connection new connection to set
         */
        void update(Connection connection);
    }

    /**
     * Default implementation using MethodContext for connection storage.
     */
    protected static class DefaultConnectionUpdater implements ConnectionUpdater {

        private final MethodContext context;

        DefaultConnectionUpdater(MethodContext context) {
            this.context = context;
        }

        @Override
        public Connection getConnection() {
            return context.getResult();
        }

        @Override
        public void update(Connection connection) {
            context.setResult(connection);
        }
    }

}
