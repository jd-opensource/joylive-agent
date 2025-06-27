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
package com.jd.live.agent.governance.db;

/**
 * Cluster-aware database connection with auto-close support.
 */
public interface DbConnection extends AutoCloseable {

    /**
     * Gets the connection's failover address.
     */
    DbFailover getFailover();

    /**
     * Checks if connection is closed (default: false).
     */
    default boolean isClosed() {
        return false;
    }

    /**
     * Attempts to reconnect to specified cluster address.
     *
     * @param newAddress target cluster address
     * @return failover response (default: NONE)
     */
    default DbFailoverResponse failover(DbAddress newAddress) {
        return DbFailoverResponse.NONE;
    }
}