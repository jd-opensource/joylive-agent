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
import com.jd.live.agent.plugin.failover.lettuce.v6.util.UriUtils;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.push.RedisClusterPushListener;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.protocol.ConnectionIntent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

public class LettuceStatefulRedisClusterConnection<K, V>
        extends LettuceStatefulConnection<K, V, StatefulRedisClusterConnection<K, V>, Iterable<RedisURI>>
        implements StatefulRedisClusterConnection<K, V> {

    private final RedisURI firstUri;

    public LettuceStatefulRedisClusterConnection(StatefulRedisClusterConnection<K, V> delegate,
                                                 Iterable<RedisURI> uris,
                                                 ClusterRedirect address,
                                                 Consumer<DbConnection> closer,
                                                 Function<Iterable<RedisURI>, CompletionStage<StatefulRedisClusterConnection<K, V>>> recreator) {
        super(delegate, uris, address, closer, recreator);
        this.firstUri = uris.iterator().next();
    }

    @Override
    public RedisAdvancedClusterCommands<K, V> sync() {
        return delegate.sync();
    }

    @Override
    public RedisAdvancedClusterAsyncCommands<K, V> async() {
        return delegate.async();
    }

    @Override
    public RedisAdvancedClusterReactiveCommands<K, V> reactive() {
        return delegate.reactive();
    }

    @Override
    public StatefulRedisConnection<K, V> getConnection(String nodeId, ConnectionIntent connectionIntent) {
        return delegate.getConnection(nodeId, connectionIntent);
    }

    @Override
    public CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(String nodeId, ConnectionIntent connectionIntent) {
        return delegate.getConnectionAsync(nodeId, connectionIntent);
    }

    @Override
    public StatefulRedisConnection<K, V> getConnection(String host, int port, ConnectionIntent connectionIntent) {
        return delegate.getConnection(host, port, connectionIntent);
    }

    @Override
    public CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(String host, int port, ConnectionIntent connectionIntent) {
        return delegate.getConnectionAsync(host, port, connectionIntent);
    }

    @Override
    public void setReadFrom(ReadFrom readFrom) {
        delegate.setReadFrom(readFrom);
    }

    @Override
    public ReadFrom getReadFrom() {
        return delegate.getReadFrom();
    }

    @Override
    public Partitions getPartitions() {
        return delegate.getPartitions();
    }

    @Override
    public RedisChannelWriter getChannelWriter() {
        return delegate.getChannelWriter();
    }

    @Override
    public void addListener(RedisClusterPushListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(RedisClusterPushListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    protected Iterable<RedisURI> getUri(ClusterAddress newAddress) {
        return UriUtils.getClusterUris(firstUri, newAddress.getNodes());
    }

    @Override
    protected void closeOld() {
        // TODO close old
    }
}
