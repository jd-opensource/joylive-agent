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

import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import lombok.Getter;

import java.util.Objects;
import java.util.function.Function;

import static com.jd.live.agent.governance.db.DbAddress.TYPE_DB;

/**
 * Thread-safe utility for managing database address failover.
 *
 * <p>Maintains both thread-local and global redirect mappings with atomic updates.
 * Primarily used for database failover scenarios.
 */
@Getter
public class DbFailover {

    private static final Function<LiveDatabase, DbAddress> DB_ADDRESS_RESOLVER = database -> new DbAddress(TYPE_DB, database.getPrimaryAddress());

    private final String type;

    private final AccessMode accessMode;

    private final DbAddress oldAddress;

    private final DbAddress newAddress;

    private final Function<LiveDatabase, DbAddress> addressResolver;

    public DbFailover(String type,
                      AccessMode accessMode,
                      String oldAddress,
                      String newAddress,
                      Function<LiveDatabase, DbAddress> addressResolver) {
        this(type, accessMode, new DbAddress(type, oldAddress), new DbAddress(type, newAddress), addressResolver);
    }

    public DbFailover(String type,
                      AccessMode accessMode,
                      DbAddress oldAddress,
                      DbAddress newAddress,
                      Function<LiveDatabase, DbAddress> addressResolver) {
        this.type = type == null && newAddress != null ? newAddress.getType() : TYPE_DB;
        this.accessMode = accessMode == null ? AccessMode.READ_WRITE : accessMode;
        this.oldAddress = oldAddress;
        this.newAddress = newAddress;
        this.addressResolver = addressResolver;
    }

    public DbFailover newAddress(DbAddress newAddress) {
        return new DbFailover(type, accessMode, oldAddress, newAddress, addressResolver);
    }

    public boolean isRedirected() {
        return !oldAddress.equals(newAddress);
    }

    /**
     * Checks if the candidate's new address differs from current failover target.
     *
     * @param candidate configuration to compare against
     * @return true if addresses differ or candidate is null, false otherwise
     */
    public boolean isChanged(DbCandidate candidate) {
        if (candidate == null) {
            return true;
        }
        return Objects.equals(newAddress.getAddress(), candidate.getNewAddress());
    }

    /**
     * Creates a {@link DbFailover} instance from a candidate configuration.
     *
     * @param candidate configuration containing failover parameters
     * @return new failover instance with resolved target address
     */
    public static DbFailover of(DbCandidate candidate) {
        return candidate == null ? null : new DbFailover(
                candidate.getType(),
                candidate.getAccessMode(),
                candidate.getOldAddress(),
                candidate.getNewAddress(),
                database -> new DbAddress(candidate.getType(), candidate.getAddressResolver().apply(database)));
    }

}
