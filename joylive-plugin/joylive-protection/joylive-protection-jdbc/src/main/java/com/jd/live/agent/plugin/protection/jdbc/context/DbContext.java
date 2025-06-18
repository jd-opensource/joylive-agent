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
package com.jd.live.agent.plugin.protection.jdbc.context;

import com.jd.live.agent.governance.util.network.ClusterRedirect;

import javax.sql.DataSource;

/**
 * Thread-local context for database operations.
 */
public class DbContext {

    private static final ThreadLocal<ContextHolder> DATA_SOURCE_LOCAL = new ThreadLocal<>();

    /**
     * @return Current thread's DataSource (null if unset)
     */
    public static DataSource getDataSource() {
        ContextHolder holder = DATA_SOURCE_LOCAL.get();
        return holder == null ? null : holder.dataSource;
    }

    /**
     * Binds a DataSource to current thread
     *
     * @param dataSource Target connection pool
     */
    public static void setDataSource(DataSource dataSource) {
        ContextHolder holder = getOrCreateHolder();
        holder.dataSource = dataSource;
    }

    /**
     * @return Active cluster redirection config (null if unset)
     */
    public static ClusterRedirect getClusterRedirect() {
        ContextHolder holder = DATA_SOURCE_LOCAL.get();
        return holder == null ? null : holder.redirect;
    }

    /**
     * Sets cluster redirection rules for current thread
     *
     * @param redirect Failover/redirect configuration
     */
    public static void setClusterRedirect(ClusterRedirect redirect) {
        ContextHolder holder = getOrCreateHolder();
        holder.redirect = redirect;
    }

    /**
     * Clears all thread-bound database context
     */
    public static void clear() {
        DATA_SOURCE_LOCAL.remove();
    }

    /**
     * Gets or creates the thread-local context holder.
     *
     * @return Existing holder for current thread, or new holder if none exists
     * @implNote Thread-safe: Each thread maintains its own isolated instance
     */
    private static ContextHolder getOrCreateHolder() {
        ContextHolder holder = DATA_SOURCE_LOCAL.get();
        if (holder == null) {
            holder = new ContextHolder();
            DATA_SOURCE_LOCAL.set(holder);
        }
        return holder;
    }

    private static class ContextHolder {

        private DataSource dataSource;

        private ClusterRedirect redirect;

    }
}
