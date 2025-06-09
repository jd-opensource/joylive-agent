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
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.splitList;
import static com.jd.live.agent.core.util.time.Timer.getRetryInterval;

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

    protected final Timer timer;

    protected final Map<ClusterAddress, List<C>> connections = new ConcurrentHashMap<>();

    protected final Map<C, FailoverTask> tasks = new ConcurrentHashMap<>(128);

    protected final Consumer<C> closer = c -> {
        List<C> values = connections.get(c.getAddress().getNewAddress());
        if (values != null) {
            values.remove(c);
        }
    };

    public AbstractDbConnectionInterceptor(PolicySupplier policySupplier, Publisher<DatabaseEvent> publisher, Timer timer) {
        this.policySupplier = policySupplier;
        this.publisher = publisher;
        this.timer = timer;
        publisher.addHandler(this::onEvent);
    }

    /**
     * Retrieves the master database node for the given address.
     *
     * @param address the target cluster address
     * @return DbResult with master info, or null if no master available
     */
    protected DbResult getMaster(String address) {
        address = address == null ? null : address.toLowerCase();
        GovernancePolicy policy = policySupplier.getPolicy();
        String[] nodes = toList(toList(splitList(address), URI::parse), URI::getAddress).toArray(new String[0]);
        // get master is case sensitive
        LiveDatabase database = policy.getMaster(nodes);
        return database == null ? null : new DbResult(address, nodes, database);
    }

    /**
     * Checks if master database configuration has changed between two states.
     *
     * @param oldResult previous database state (may be null)
     * @param newResult current database state (may be null)
     * @return true if master addresses differ or state changed from/to null
     */
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
                // primary address is lowercase.
                ClusterAddress newAddress = createAddress(master.getPrimaryAddress());
                // Close connection to reconnect to the new master address
                cons.forEach(c -> {
                    if (!c.getAddress().getNewAddress().equals(newAddress)) {
                        // avoid async concurrently updating.
                        while (!c.isClosed()) {
                            FailoverTask newTask = new FailoverTask(c, newAddress);
                            FailoverTask oldTask = tasks.putIfAbsent(c, newTask);
                            if (oldTask == null) {
                                timer.delay("redirect-connection", getRetryInterval(100, 2000), newTask);
                            } else if (oldTask.add(newAddress)) {
                                break;
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * Creates a new ClusterAddress from the given string representation.
     *
     * @param address the address string to parse
     * @return newly created ClusterAddress (never null)
     */
    protected ClusterAddress createAddress(String address) {
        return new ClusterAddress(address);
    }

    /**
     * Redirects the given connection to the specified cluster address.
     *
     * @param connection the connection to redirect
     * @param address    the target cluster address
     */
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

    /**
     * A background task that handles connection failover by redirecting to alternate cluster addresses.
     * Thread-safe operations are guarded by the internal mutex lock.
     */
    protected class FailoverTask implements Runnable {

        protected final C connection;

        protected final Deque<ClusterAddress> queue = new LinkedList<>();

        protected final Object mutex = new Object();

        protected volatile boolean closed;

        public FailoverTask(C connection, ClusterAddress address) {
            this.connection = connection;
            queue.add(address);
        }

        /**
         * Adds a new failover target address to the queue.
         *
         * @param address the alternate address to add
         * @return true if added successfully, false if task was already closed
         */
        public boolean add(ClusterAddress address) {
            synchronized (mutex) {
                if (closed) {
                    return false;
                }
                queue.add(address);
            }
            return true;
        }

        @Override
        public void run() {
            synchronized (mutex) {
                ClusterAddress address = queue.getLast();
                queue.clear();
                try {
                    redirectTo(connection, address);
                } finally {
                    closed = true;
                    tasks.remove(connection);
                }
            }
        }
    }

}
