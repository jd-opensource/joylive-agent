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
package com.jd.live.agent.plugin.protection.hikaricp.datasource;

import com.jd.live.agent.governance.db.DbUrl;
import com.jd.live.agent.governance.db.jdbc.datasource.LiveDataSource;
import com.jd.live.agent.plugin.protection.hikaricp.connection.HikariPooledConnection;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * HikariCP implementation of {@link LiveDataSource}.
 * Provides live management for Hikari connection pools.
 */
public class HikariLiveDataSource implements LiveDataSource {

    private final HikariDataSource dataSource;

    private final DbUrl dbUrl;

    private HikariPool pool;

    private Method evictMethod;

    public HikariLiveDataSource(HikariDataSource dataSource, DbUrl dbUrl) {
        this.dataSource = dataSource;
        this.dbUrl = dbUrl;
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public String getPoolName() {
        return dataSource.getPoolName();
    }

    @Override
    public DbUrl getUrl() {
        return dbUrl;
    }

    @Override
    public void evict(Connection connection) {
        if (connection instanceof HikariPooledConnection) {
            Object poolEntry = ((HikariPooledConnection) connection).getPoolEntry();
            HikariPool pool = getPool();
            Method evictMethod = getEvictMethod();
            if (pool != null) {
                try {
                    evictMethod.invoke(pool, poolEntry, "(connection evicted by user)", !connection.isClosed());
                } catch (Throwable ignored) {
                    // ignore
                }
            }
        }
    }

    @Override
    public boolean validate() {
        return dbUrl != null && dbUrl.hasAddress();
    }

    /**
     * Gets the underlying HikariPool instance, initializing if needed.
     * Uses reflection to safely access the pool field.
     *
     * @return the managed HikariPool instance
     */
    protected HikariPool getPool() {
        if (pool == null) {
            pool = getQuietly(dataSource, "pool");
        }
        return pool;
    }

    /**
     * Gets the soft-eviction method via reflection.
     * Caches the method reference after first lookup.
     *
     * @return the accessible softEvictConnection Method
     */
    protected Method getEvictMethod() {
        if (evictMethod == null) {
            Method[] methods = HikariPool.class.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("softEvictConnection")) {
                    method.setAccessible(true);
                    evictMethod = method;
                    break;
                }
            }
        }
        return evictMethod;
    }
}
