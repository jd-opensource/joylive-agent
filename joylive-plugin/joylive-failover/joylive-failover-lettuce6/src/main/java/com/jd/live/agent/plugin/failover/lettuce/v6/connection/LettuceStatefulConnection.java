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
package com.jd.live.agent.plugin.failover.lettuce.v6.connection;

import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import lombok.Getter;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class LettuceStatefulConnection<K, V, T extends StatefulConnection<K, V>, U>
        implements StatefulConnection<K, V>, LettuceConnection {

    protected volatile T delegate;
    protected volatile U uri;
    @Getter
    protected volatile ClusterRedirect address;
    protected final Consumer<DbConnection> closer;
    protected final Function<U, CompletionStage<T>> recreator;
    protected final AtomicBoolean closed = new AtomicBoolean(false);
    protected final Object mutex = new Object();

    public LettuceStatefulConnection(T delegate,
                                     U uri,
                                     ClusterRedirect address,
                                     Consumer<DbConnection> closer,
                                     Function<U, CompletionStage<T>> recreator) {
        this.delegate = delegate;
        this.uri = uri;
        this.address = address;
        this.closer = closer;
        this.recreator = recreator;
    }

    @Override
    public void addListener(RedisConnectionStateListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(RedisConnectionStateListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    public void setTimeout(Duration timeout) {
        delegate.setTimeout(timeout);
    }

    @Override
    public Duration getTimeout() {
        return delegate.getTimeout();
    }

    @Override
    public <T> RedisCommand<K, V, T> dispatch(RedisCommand<K, V, T> command) {
        return delegate.dispatch(command);
    }

    @Override
    public Collection<RedisCommand<K, V, ?>> dispatch(Collection<? extends RedisCommand<K, V, ?>> redisCommands) {
        return delegate.dispatch(redisCommands);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            delegate.close();
            if (closer != null) {
                closer.accept(this);
            }
        }
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return delegate.closeAsync();
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public ClientOptions getOptions() {
        return delegate.getOptions();
    }

    @Override
    public ClientResources getResources() {
        return delegate.getResources();
    }

    @Override
    @Deprecated
    public void reset() {
        delegate.reset();
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        delegate.setAutoFlushCommands(autoFlush);
    }

    @Override
    public void flushCommands() {
        delegate.flushCommands();
    }

    /**
     * Handles cluster redirection by updating connection to the specified node.
     *
     * @param newAddress Target cluster node address for redirection
     * @return New cluster redirect information after update
     */
    public ClusterRedirect redirect(ClusterAddress newAddress) {
        ClusterRedirect newRedirect = address.newAddress(newAddress);
        U newUri = getUri(newAddress);
        address = newRedirect;
        uri = newUri;
        synchronized (mutex) {
            delegate.close();
        }
        closeOld();
        reconnect(newUri);
        return address;
    }

    protected void closeOld() {
    }

    protected abstract U getUri(ClusterAddress newAddress);

    /**
     * Verifies if reconnection to specified URI should be attempted.
     *
     * @param uri Redis URI to validate
     * @return true if connection is active and URI matches current configuration
     */
    private boolean isReconnectable(U uri) {
        return !isClosed() && uri == this.uri;
    }

    /**
     * Asynchronously reconnects to specified Redis URI and updates active connection.
     *
     * @param uri Target Redis connection parameters
     */
    private void reconnect(U uri) {
        CompletionStage<T> future = recreator.apply(uri);
        future.whenComplete((conn, throwable) -> {
            if (throwable == null) {
                synchronized (mutex) {
                    if (isReconnectable(uri)) {
                        delegate = conn;
                    } else {
                        conn.close();
                    }
                }
            } else if (isReconnectable(uri)) {
                reconnect(uri);
            }
        });
    }

}
