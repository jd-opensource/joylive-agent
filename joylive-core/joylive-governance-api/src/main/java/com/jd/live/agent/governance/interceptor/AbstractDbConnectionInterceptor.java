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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.splitList;

/**
 * Abstract base class for database connection interceptors with failover support.
 *
 * <p>Manages connection pools and automatically redirects connections when database
 * topology changes (e.g., master failover). Concrete implementations must provide
 * actual connection wrapping logic.
 *
 * @param <C> Wrapped connection type (must be AutoCloseable)
 */
public abstract class AbstractDbConnectionInterceptor<C extends DbConnection> extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDbConnectionInterceptor.class);

    protected static final String ATTR_OLD_ADDRESS = "oldAddress";

    protected static final BiConsumer<ClusterAddress, ClusterAddress> consumer = (oldAddress, newAddress) -> logger.info("{} connection is redirected from {} to {} ", oldAddress.getType(), oldAddress, newAddress);

    protected final PolicySupplier policySupplier;

    protected final Publisher<DatabaseEvent> publisher;

    protected final Map<ClusterAddress, List<C>> connections = new ConcurrentHashMap<>();

    protected final Consumer<C> closer = c -> {
        List<C> values = connections.get(c.getAddress().getNewAddress());
        if (values != null) {
            values.remove(c);
        }
    };

    public AbstractDbConnectionInterceptor(PolicySupplier policySupplier, Publisher<DatabaseEvent> publisher) {
        this.policySupplier = policySupplier;
        this.publisher = publisher;
        publisher.addHandler(this::onEvent);
    }

    protected DbResult getMaster(String address) {
        GovernancePolicy policy = policySupplier.getPolicy();
        String[] nodes = toList(toList(splitList(address), URI::parse), URI::getAddress).toArray(new String[0]);
        LiveDatabase database = policy.getMaster(nodes);
        return database == null ? null : new DbResult(address, nodes, database);
    }

    protected boolean isChanged(DbResult oldResult, DbResult newResult) {
        if (oldResult == null) {
            return newResult != null;
        } else if (newResult == null) {
            return true;
        }
        return !Objects.equals(oldResult.getMaster().getAddresses(), newResult.getMaster().getAddresses());
    }

    /**
     * Creates and tracks a wrapped connection.
     *
     * @param supplier The connection supplier
     * @return Managed connection instance
     */
    protected C createConnection(Supplier<C> supplier) {
        C conn = supplier.get();
        addConnection(conn);
        return conn;
    }

    protected void addConnection(C conn) {
        if (conn != null) {
            ClusterAddress address = conn.getAddress().getNewAddress();
            connections.computeIfAbsent(address, a -> new CopyOnWriteArrayList<>()).add(conn);
        }
    }

    /**
     * Handles database topology change events.
     *
     * @param events List of database change notifications
     */
    protected void onEvent(List<Event<DatabaseEvent>> events) {
        GovernancePolicy policy = policySupplier.getPolicy();
        connections.forEach((address, cons) -> {
            if (cons.isEmpty()) {
                return;
            }
            LiveDatabase master = policy.getMaster(address.getNodes());
            if (master != null && !master.contains(address.getNodes())) {
                ClusterAddress newAddress = createAddress(master.getPrimaryAddress());
                // Close connection to reconnect to the new master address
                cons.forEach(c -> {
                    if (!c.getAddress().getNewAddress().equals(newAddress)) {
                        redirectTo(c, newAddress);
                    }
                });
            }
        });
    }

    protected ClusterAddress createAddress(String address) {
        return new ClusterAddress(address);
    }

    protected abstract void redirectTo(C connection, ClusterAddress address);

    @Getter
    protected static class DbResult {

        private final String oldAddress;

        private final String[] oldNodes;

        private final LiveDatabase master;

        @Setter
        private String newAddress;

        public DbResult(String oldAddress, String[] oldNodes, LiveDatabase master) {
            this.oldAddress = oldAddress;
            this.oldNodes = oldNodes;
            this.master = master;
        }

        public boolean isMaster() {
            return master != null && master.contains(oldNodes);
        }

    }

}
