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

import com.jd.live.agent.core.event.Event;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.db.DbUrlParser;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
public abstract class AbstractDbConnectionInterceptor<C extends DbConnection> extends AbstractDbFailoverInterceptor {

    protected final Publisher<DatabaseEvent> publisher;

    protected final Timer timer;

    protected final Map<String, DbUrlParser> parsers;

    protected final Map<ClusterAddress, Set<C>> connections = new ConcurrentHashMap<>();

    protected final Map<C, FailoverTask> tasks = new ConcurrentHashMap<>(128);

    protected final Consumer<C> closer = c -> {
        Set<C> values = connections.get(c.getAddress().getNewAddress());
        if (values != null) {
            values.remove(c);
        }
    };

    public AbstractDbConnectionInterceptor(InvocationContext context) {
        super(context);
        this.publisher = context.getDatabasePublisher();
        this.timer = context.getTimer();
        this.parsers = context.getDbUrlParsers();
        publisher.addHandler(this::onEvent);
    }

    /**
     * Creates a new connection using the supplier and tracks it.
     * @param supplier provides raw connections
     * @return the managed connection
     */
    protected C createConnection(Supplier<C> supplier) {
        C conn = supplier.get();
        addConnection(conn);
        return conn;
    }

    /**
     * Tracks a connection in the connection pool.
     * Skips null connections. Groups by cluster address.
     *
     * @param conn the connection to track
     */
    protected void addConnection(C conn) {
        if (conn != null) {
            ClusterAddress address = conn.getAddress().getNewAddress();
            connections.computeIfAbsent(address, a -> new CopyOnWriteArraySet<>()).add(conn);
        }
    }

    /**
     * Handles database topology change events.
     *
     * @param events List of database change notifications
     */
    protected void onEvent(List<Event<DatabaseEvent>> events) {
        if (!isInteresting(events)) {
            return;
        }
        GovernancePolicy policy = policySupplier.getPolicy();
        connections.forEach((address, cons) -> {
            if (cons.isEmpty()) {
                return;
            }
            LiveDatabase oldDatabase = policy.getDatabase(address.getNodes());
            LiveDatabase newDatabase = oldDatabase.getReadDatabase(location.getUnit(), location.getCell());
            LiveDatabase master = oldDatabase.getWriteDatabase();
            cons.forEach(c -> {
                ClusterRedirect redirect = c.getAddress();
                String[] nodes = redirect.getNewAddress().getNodes();
                AccessMode accessMode = redirect.getAccessMode();
                if (accessMode.isWriteable() && master != null && master != oldDatabase && !master.contains(nodes)) {
                    // redirect when master is changed.
                    addTask(c, master);
                } else if (!accessMode.isWriteable() && newDatabase != null && newDatabase != oldDatabase && !newDatabase.contains(nodes)) {
                    // redirect when slave is changed.
                    addTask(c, newDatabase);
                }
            });
        });
    }

    /**
     * Checks if any event in the list is relevant to this instance.
     * An event is relevant if its owner is null or matches this instance.
     *
     * @param events List of database events to check
     * @return true if any relevant event is found, false otherwise
     */
    protected boolean isInteresting(List<Event<DatabaseEvent>> events) {
        for (Event<DatabaseEvent> event : events) {
            DatabaseEvent de = event.getData();
            Object owner = de.getOwner();
            if (owner == null || owner == this) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a failover task for connection redirection with collision handling.
     *
     * @param conn     the connection needing redirection
     * @param database target database with new connection details
     */
    protected void addTask(C conn, LiveDatabase database) {
        // avoid async concurrently updating.
        ClusterRedirect redirect = conn.getAddress();
        ClusterAddress newAddress = redirect.getAddressResolver().apply(database);
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
     * Redirects the given connection to the specified cluster address.
     *
     * @param connection the connection to redirect
     * @param address    the target cluster address
     */
    protected abstract void redirectTo(C connection, ClusterAddress address);

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
