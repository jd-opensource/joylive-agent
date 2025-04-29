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
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import com.jd.live.agent.governance.util.RedirectAddress;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Abstract base class for database connection interceptors with failover support.
 *
 * <p>Manages connection pools and automatically redirects connections when database
 * topology changes (e.g., master failover). Concrete implementations must provide
 * actual connection wrapping logic.
 *
 * @param <T> Raw connection type to intercept
 * @param <C> Wrapped connection type (must be AutoCloseable)
 */
public abstract class AbstractDbConnectionInterceptor<T, C extends AutoCloseable> extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDbConnectionInterceptor.class);

    protected static final BiConsumer<String, String> consumer = (oldAddress, newAddress) -> logger.info("DB connection is redirected from {} to {} ", oldAddress, newAddress);

    protected final PolicySupplier policySupplier;

    protected final Publisher<DatabaseEvent> publisher;

    protected final Map<String, List<C>> connections = new ConcurrentHashMap<>();

    public AbstractDbConnectionInterceptor(PolicySupplier policySupplier, Publisher<DatabaseEvent> publisher) {
        this.policySupplier = policySupplier;
        this.publisher = publisher;
        publisher.addHandler(this::onEvent);
    }

    /**
     * Creates and tracks a wrapped connection.
     *
     * @param connection Raw connection to wrap
     * @param address    Database endpoint address
     * @return Managed connection instance
     */
    protected C createConnection(T connection, RedirectAddress address) {
        String newAddress = address.getNewAddress();
        C conn = doCreateConnection(connection, address, c -> {
            List<C> values = connections.get(newAddress);
            if (values != null) {
                values.remove(c);
            }
        });
        connections.computeIfAbsent(newAddress, a -> new CopyOnWriteArrayList<>()).add(conn);
        return conn;
    }

    /**
     * Implementation hook for connection wrapping.
     *
     * @param connection Raw connection to wrap
     * @param address    Database endpoint address
     * @param close      Callback to remove connection from pool
     * @return Custom wrapped connection
     */
    protected abstract C doCreateConnection(T connection, RedirectAddress address, Consumer<C> close);

    /**
     * Handles database topology change events.
     *
     * @param events List of database change notifications
     */
    protected void onEvent(List<Event<DatabaseEvent>> events) {
        GovernancePolicy policy = policySupplier.getPolicy();
        Close close = Close.instance();
        connections.forEach((address, cons) -> {
            if (cons.isEmpty()) {
                return;
            }
            LiveDatabase master = policy.getMaster(address);
            if (master != null && !master.contains(address)) {
                String newAddress = master.getPrimaryAddress();
                // Close connection to reconnect to the new master address
                cons.forEach(c -> {
                    close.close(c);
                    onRedirect(c, newAddress);
                });
            }
        });
    }

    protected abstract void onRedirect(C connection, String address);

}
