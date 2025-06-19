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
package com.jd.live.agent.governance.util.network;

import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.jd.live.agent.governance.util.network.ClusterAddress.TYPE_DB;

/**
 * Thread-safe utility for managing database address redirections.
 *
 * <p>Maintains both thread-local and global redirect mappings with atomic updates.
 * Primarily used for database failover scenarios.
 */
@Getter
public class ClusterRedirect {

    private static final Map<ClusterAddress, AtomicReference<ClusterAddress>> REDIRECTS = new ConcurrentHashMap<>();

    public static final Function<LiveDatabase, ClusterAddress> DB_ADDRESS_RESOLVER = database -> new ClusterAddress(TYPE_DB, database.getPrimaryAddress());

    private final String type;

    private final AccessMode accessMode;

    private final ClusterAddress oldAddress;

    private final ClusterAddress newAddress;

    private final Function<LiveDatabase, ClusterAddress> addressResolver;

    public ClusterRedirect(String address) {
        this(TYPE_DB, AccessMode.READ_WRITE, new ClusterAddress(TYPE_DB, address), new ClusterAddress(TYPE_DB, address), DB_ADDRESS_RESOLVER);
    }

    public ClusterRedirect(String oldAddress, String newAddress) {
        this(TYPE_DB, AccessMode.READ_WRITE, new ClusterAddress(TYPE_DB, oldAddress), new ClusterAddress(TYPE_DB, newAddress), DB_ADDRESS_RESOLVER);
    }

    public ClusterRedirect(String type, String oldAddress, String newAddress, Function<LiveDatabase, ClusterAddress> addressResolver) {
        this(type, AccessMode.READ_WRITE, new ClusterAddress(type, oldAddress), new ClusterAddress(type, newAddress), addressResolver);
    }

    public ClusterRedirect(String type,
                           AccessMode accessMode,
                           String oldAddress,
                           String newAddress,
                           Function<LiveDatabase, ClusterAddress> addressResolver) {
        this(type, accessMode, new ClusterAddress(type, oldAddress), new ClusterAddress(type, newAddress), addressResolver);
    }

    public ClusterRedirect(String type,
                           ClusterAddress oldAddress,
                           ClusterAddress newAddress,
                           Function<LiveDatabase, ClusterAddress> addressResolver) {
        this(type, AccessMode.READ_WRITE, oldAddress, newAddress, addressResolver);
    }

    public ClusterRedirect(String type,
                           AccessMode accessMode,
                           ClusterAddress oldAddress,
                           ClusterAddress newAddress,
                           Function<LiveDatabase, ClusterAddress> addressResolver) {
        this.type = type == null && newAddress != null ? newAddress.getType() : TYPE_DB;
        this.accessMode = accessMode == null ? AccessMode.READ_WRITE : accessMode;
        this.oldAddress = oldAddress;
        this.newAddress = newAddress;
        this.addressResolver = addressResolver;
    }

    public ClusterRedirect newAddress(ClusterAddress newAddress) {
        return new ClusterRedirect(type, accessMode, oldAddress, newAddress, addressResolver);
    }

    /**
     * Gets the redirected address for a given cluster address if one exists.
     *
     * @param address the original cluster address to check
     * @return the redirected address, or null if no redirect exists
     */
    public static ClusterAddress getRedirect(ClusterAddress address) {
        AtomicReference<ClusterAddress> reference = REDIRECTS.get(address);
        return reference == null ? null : reference.get();
    }

    /**
     * Atomically updates global redirect mapping
     *
     * @param address  Original database address
     * @param callback Notification when redirection occurs
     */
    public static void redirect(ClusterRedirect address, BiConsumer<ClusterAddress, ClusterAddress> callback) {
        AtomicReference<ClusterAddress> reference = REDIRECTS.computeIfAbsent(address.getOldAddress(), AtomicReference::new);
        ClusterAddress oldRedirect = reference.get();
        ClusterAddress newRedirect = address.getNewAddress();
        if (!newRedirect.equals(oldRedirect) && reference.compareAndSet(oldRedirect, newRedirect)) {
            if (callback != null) {
                callback.accept(oldRedirect, newRedirect);
            }
        }
    }

}
