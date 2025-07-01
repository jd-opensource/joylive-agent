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
import com.jd.live.agent.governance.db.DbCandidate;
import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.governance.db.DbUrlParser;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

    protected final Consumer<C> closer;

    public AbstractDbConnectionInterceptor(InvocationContext context) {
        super(context);
        this.publisher = context.getDatabasePublisher();
        this.timer = context.getTimer();
        this.parsers = context.getDbUrlParsers();
        this.closer = connectionSupervisor::removeConnection;
        publisher.addHandler(this::onEvent);
    }

    /**
     * Creates a new connection using the supplier and tracks it.
     * @param supplier provides raw connections
     * @return the managed connection
     */
    protected C createConnection(Supplier<C> supplier) {
        return connectionSupervisor.addConnection(supplier.get());
    }

    /**
     * Handles cluster connection verification and redirection flow.
     *
     * @param connection      Cluster connection to verify
     * @param addressResolver Function to resolve database addresses
     */
    protected C checkFailover(C connection, Function<LiveDatabase, String> addressResolver) {
        if (connection != null) {
            checkFailover(connectionSupervisor.failover(connection.getFailover()), addressResolver);
        }
        return connection;
    }

    /**
     * Detects address changes by comparing current failover with new candidate.
     * Publishes event if changes are detected.
     *
     * @param failover        current cluster redirect configuration
     * @param addressResolver strategy to resolve database addresses
     */
    protected void checkFailover(DbFailover failover, Function<LiveDatabase, String> addressResolver) {
        // Avoid missing events caused by synchronous changes
        DbCandidate newCandidate = connectionSupervisor.getCandidate(failover, addressResolver);
        if (failover.isChanged(newCandidate)) {
            publisher.offer(new DatabaseEvent(this));
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
        connectionSupervisor.failover();
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
            Object receiver = de.getReceiver();
            if (receiver == null || receiver == this) {
                return true;
            }
        }
        return false;
    }

}
