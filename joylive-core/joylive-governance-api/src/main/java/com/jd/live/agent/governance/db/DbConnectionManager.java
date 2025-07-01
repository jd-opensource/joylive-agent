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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import lombok.Getter;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.jd.live.agent.core.util.time.Timer.getRetryInterval;

/**
 * Default implementation of {@link DbConnectionSupervisor}.
 * Manages database connections lifecycle and failover operations.
 */
public class DbConnectionManager implements DbConnectionSupervisor {

    private static final Logger logger = LoggerFactory.getLogger(DbConnectionManager.class);

    private final PolicySupplier policySupplier;

    private final Location location;

    private final Timer timer;

    private final Map<DbAddress, AtomicReference<DbAddress>> failovers = new ConcurrentHashMap<>();

    private final BiConsumer<DbAddress, DbAddress> consumer = (oldAddress, newAddress) -> logger.info("{} connection is redirected from {} to {} ", oldAddress.getType(), oldAddress, newAddress);

    private final Map<DbAddress, Set<DbConnection>> connections = new ConcurrentHashMap<>();

    private final Map<DbConnection, FailoverTask> tasks = new ConcurrentHashMap<>(128);

    public DbConnectionManager(PolicySupplier policySupplier, Location location, Timer timer) {
        this.policySupplier = policySupplier;
        this.location = location;
        this.timer = timer;
    }

    @Override
    public <C extends DbConnection> C addConnection(C connection, DbAddress address) {
        if (address != null && connection != null) {
            connections.computeIfAbsent(address, a -> new CopyOnWriteArraySet<>()).add(connection);
        }
        return connection;
    }

    @Override
    public <C extends DbConnection> C removeConnection(C conn) {
        return conn == null ? null : removeConnection(conn, conn.getFailover().getNewAddress());
    }

    @Override
    public <C extends DbConnection> C removeConnection(C conn, DbAddress address) {
        if (conn != null && address != null) {
            Set<DbConnection> values = connections.get(address);
            if (values != null) {
                values.remove(conn);
            }
        }
        return conn;
    }

    @Override
    public DbAddress getFailover(DbAddress address) {
        AtomicReference<DbAddress> reference = failovers.get(address);
        return reference == null ? null : reference.get();
    }

    @Override
    public DbFailover failover(DbFailover failover) {
        if (failover != null) {
            failover(failover, consumer);
        }
        return failover;
    }

    @Override
    public void failover(DbFailover failover, BiConsumer<DbAddress, DbAddress> callback) {
        if (failover == null) {
            return;
        }
        AtomicReference<DbAddress> reference = failovers.computeIfAbsent(failover.getOldAddress(), AtomicReference::new);
        DbAddress oldAddress = reference.get();
        DbAddress newAddress = failover.getNewAddress();
        if (!newAddress.equals(oldAddress) && reference.compareAndSet(oldAddress, newAddress)) {
            if (callback != null) {
                callback.accept(oldAddress, newAddress);
            }
        }
    }

    @Override
    public void failover() {
        GovernancePolicy policy = policySupplier.getPolicy();
        connections.forEach((address, cons) -> {
            if (cons.isEmpty()) {
                return;
            }
            LiveDatabase oldDatabase = policy.getDatabase(address.getNodes());
            if (oldDatabase != null) {
                LiveDatabase newDatabase = oldDatabase.getReadDatabase(location.getUnit(), location.getCell());
                LiveDatabase master = oldDatabase.getWriteDatabase();
                cons.forEach(c -> {
                    DbFailover failover = c.getFailover();
                    String[] nodes = failover.getNewAddress().getNodes();
                    AccessMode accessMode = failover.getAccessMode();
                    if (accessMode.isWriteable() && master != null && master != oldDatabase && !master.contains(nodes)) {
                        // redirect when master is changed.
                        addTask(c, master);
                    } else if (!accessMode.isWriteable() && newDatabase != null && newDatabase != oldDatabase && !newDatabase.contains(nodes)) {
                        // redirect when slave is changed.
                        addTask(c, newDatabase);
                    }
                });
            }
        });
    }

    @Override
    public DbCandidate getCandidate(String type, String address, String[] nodes, AccessMode accessMode, Function<LiveDatabase, String> addressResolver) {
        GovernancePolicy policy = policySupplier.getPolicy();
        LiveDatabase database = accessMode.isWriteable()
                ? policy.getWriteDatabase(nodes)
                : policy.getReadDatabase(location.getUnit(), location.getCell(), nodes);
        return new DbCandidate(type, accessMode, address, nodes, database, addressResolver);
    }


    /**
     * Adds a failover task for connection redirection with collision handling.
     *
     * @param conn     the connection needing redirection
     * @param database target database with new connection details
     */
    protected void addTask(DbConnection conn, LiveDatabase database) {
        // avoid async concurrently updating.
        DbFailover redirect = conn.getFailover();
        DbAddress newAddress = redirect.getAddressResolver().apply(database);
        while (!conn.isClosed()) {
            FailoverTask newTask = new FailoverTask(conn, newAddress);
            FailoverTask oldTask = tasks.putIfAbsent(conn, newTask);
            if (oldTask == null) {
                timer.delay("redirect-connection", getRetryInterval(100, 2000), newTask);
                return;
            } else if (oldTask.add(newAddress)) {
                break;
            }
        }
    }

    /**
     * Adds a failover retry task for connection redirection.
     *
     * @param conn       the connection needing redirection
     * @param oldAddress the source address
     * @param newAddress the target address
     */
    protected void addRetryTask(DbConnection conn, DbAddress oldAddress, DbAddress newAddress) {
        if (!conn.isClosed() && conn.getFailover().getNewAddress().equals(oldAddress)) {
            FailoverTask newTask = new FailoverTask(conn, new DbRetryAddress(newAddress, oldAddress));
            FailoverTask oldTask = tasks.putIfAbsent(conn, newTask);
            if (oldTask == null) {
                timer.delay("redirect-connection", getRetryInterval(1000, 2000), newTask);
            }
        }
    }

    /**
     * A background task that handles connection failover by redirecting to alternate cluster addresses.
     * Thread-safe operations are guarded by the internal mutex lock.
     */
    protected class FailoverTask implements Runnable {

        private final DbConnection connection;
        private final Deque<DbAddress> queue = new LinkedList<>();
        private volatile boolean closed;

        public FailoverTask(DbConnection connection, DbAddress address) {
            this.connection = connection;
            queue.add(address);
        }

        /**
         * Adds a new failover target address to the queue.
         *
         * @param address the alternate address to add
         * @return true if added successfully, false if task was already closed
         */
        public synchronized boolean add(DbAddress address) {
            if (closed) {
                return false;
            }
            queue.add(address);
            return true;
        }

        @Override
        public synchronized void run() {
            DbAddress newAddress = queue.peekLast();
            queue.clear();
            boolean success;
            DbAddress oldAddress = connection.getFailover().getNewAddress();
            try {
                success = redirect(connection, oldAddress, newAddress);
            } finally {
                closed = true;
                tasks.remove(connection);
            }
            if (!success) {
                addRetryTask(connection, oldAddress, newAddress);
            }
        }

        /**
         * Redirects the given connection to the specified cluster address.
         *
         * @param connection the connection to redirect
         * @param oldAddress the source cluster address.
         * @param newAddress the target cluster address
         * @return true if redirection was successful, false if redirector is failed.
         */
        protected boolean redirect(DbConnection connection, DbAddress oldAddress, DbAddress newAddress) {
            // Prevent asynchronous concurrency(retry & update)， check failover address again
            if (newAddress instanceof DbRetryAddress) {
                DbRetryAddress retryAddress = (DbRetryAddress) newAddress;
                if (!retryAddress.getOldAddress().equals(oldAddress)) {
                    return true;
                }
            }
            switch (connection.failover(newAddress)) {
                case SUCCESS:
                    failover(addConnection(removeConnection(connection, oldAddress), newAddress).getFailover());
                    if (connection.isClosed()) {
                        // avoid concurrency close.
                        removeConnection(connection, newAddress);
                    }
                    return true;
                case FAILED:
                    return false;
                case DISCARD:
                    failover(removeConnection(connection, oldAddress).getFailover());
                    return true;
                case NONE:
                default:
                    return true;
            }
        }

    }

    private static class DbRetryAddress extends DbAddress {

        @Getter
        private final DbAddress oldAddress;

        DbRetryAddress(DbAddress newAddress, DbAddress oldAddress) {
            super(newAddress.getType(), newAddress.getAddress(), newAddress.getNodes());
            this.oldAddress = oldAddress;
        }
    }
}
