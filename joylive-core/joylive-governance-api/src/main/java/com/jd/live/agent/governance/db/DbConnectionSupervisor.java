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

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;

import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.join;
import static com.jd.live.agent.core.util.StringUtils.splitList;

/**
 * Manages database connections
 */
public interface DbConnectionSupervisor {

    String COMPONENT_DB_CONNECTION_SUPERVISOR = "dbConnectionSupervisor";

    /**
     * Selects an appropriate database node (master or replica) based on requested access mode.
     *
     * @param type            database type/category identifier
     * @param address         cluster address (normalized to lowercase)
     * @param accessMode      READ or WRITE operation mode
     * @param addressResolver function to resolve node address from LiveDatabase
     * @return configured DbCandidate instance, or null if no matching node available
     */
    default DbCandidate getCandidate(String type, String address, AccessMode accessMode, Function<LiveDatabase, String> addressResolver) {
        address = address == null ? null : address.toLowerCase();
        String[] nodes = toList(toList(splitList(address), URI::parse), URI::getAddress).toArray(new String[0]);
        return getCandidate(type, address, nodes, accessMode, addressResolver);
    }

    /**
     * Gets a database candidate for cluster failover.
     * Delegates to {@link #getCandidate(DbAddress, AccessMode, Function)}.
     *
     * @param failover        contains cluster address and access mode for failover
     * @param addressResolver converts LiveDatabase to connection address
     * @return configured candidate or null if no suitable node available
     */
    default DbCandidate getCandidate(DbFailover failover, Function<LiveDatabase, String> addressResolver) {
        return getCandidate(failover.getOldAddress(), failover.getAccessMode(), addressResolver);
    }

    /**
     * Selects a database cluster matching the address and access requirements.
     *
     * @param clusterAddress  cluster configuration (type/address/nodes)
     * @param accessMode      READ or WRITE operation mode
     * @param addressResolver converts LiveDatabase to connection address
     * @return configured candidate or null if unavailable
     */
    default DbCandidate getCandidate(DbAddress clusterAddress, AccessMode accessMode, Function<LiveDatabase, String> addressResolver) {
        return getCandidate(clusterAddress.getType(), clusterAddress.getAddress(), clusterAddress.getNodes(), accessMode, addressResolver);
    }

    /**
     * Selects a database cluster based on access mode and location.
     *
     * @param type            database type
     * @param nodes           available database nodes
     * @param accessMode      READ/WRITE operation mode
     * @param addressResolver converts LiveDatabase to connection address
     * @return configured database candidate (never null)
     */
    default DbCandidate getCandidate(String type, String[] nodes, AccessMode accessMode, Function<LiveDatabase, String> addressResolver) {
        return getCandidate(type, join(nodes), nodes, accessMode, addressResolver);
    }

    /**
     * Selects a database cluster based on access mode and location.
     *
     * @param type            database type
     * @param address         normalized cluster address
     * @param nodes           available database nodes
     * @param accessMode      READ/WRITE operation mode
     * @param addressResolver converts LiveDatabase to connection address
     * @return configured database candidate (never null)
     */
    DbCandidate getCandidate(String type, String address, String[] nodes, AccessMode accessMode, Function<LiveDatabase, String> addressResolver);

    /**
     * Registers a connection using its embedded address.
     *
     * @param conn connection to register
     * @param <C>  connection type
     * @return registered connection (may be wrapped)
     */
    default <C extends DbConnection> C addConnection(C conn) {
        return conn == null ? null : addConnection(conn, conn.getFailover().getNewAddress());
    }

    /**
     * Registers a connection for the given cluster address.
     *
     * @param connection the connection to register
     * @param address    target cluster address
     * @param <C>        connection type
     * @return the registered connection (may be wrapped)
     */
    <C extends DbConnection> C addConnection(C connection, DbAddress address);

    /**
     * Removes a connection from cluster supervision using its embedded address.
     *
     * @param conn connection to remove
     * @param <C>  connection type
     * @return the removed connection (same instance)
     */
    default <C extends DbConnection> C removeConnection(C conn) {
        if (conn != null) {
            removeConnection(conn, conn.getFailover().getNewAddress());
        }
        return conn;
    }

    /**
     * Removes a connection from a specific cluster address.
     *
     * @param conn    connection to remove
     * @param address target cluster address
     * @param <C>     connection type
     * @return the removed connection (same instance)
     */
    <C extends DbConnection> C removeConnection(C conn, DbAddress address);

    /**
     * Gets the current failover address for a cluster address.
     *
     * @param address original cluster address to check
     * @return redirected address, or null if no redirection exists
     */
    DbAddress getFailover(DbAddress address);

    /**
     * Creates a failover operation for a candidate.
     *
     * @param candidate source configuration with address details
     * @return configured failover instance
     */
    default DbFailover failover(DbCandidate candidate) {
        return failover(DbFailover.of(candidate));
    }

    /**
     * Updates the failover mapping for a database address.
     *
     * @param failover failover configuration containing old/new addresses
     * @return the input failover configuration
     */
    DbFailover failover(DbFailover failover);

    /**
     * Updates the failover mapping with callback notification.
     *
     * @param failover failover configuration containing old/new addresses
     * @param callback invoked when redirection occurs (receives old/new addresses)
     */
    void failover(DbFailover failover, BiConsumer<DbAddress, DbAddress> callback);

    /**
     * Initiates failover procedure for all supervised connections
     */
    void failover();
}
