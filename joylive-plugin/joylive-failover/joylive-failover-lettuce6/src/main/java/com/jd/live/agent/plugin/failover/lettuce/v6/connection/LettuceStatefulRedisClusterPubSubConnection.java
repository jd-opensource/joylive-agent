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
import com.jd.live.agent.governance.db.DbAddress;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.plugin.failover.lettuce.v6.util.UriUtils;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.cluster.api.push.RedisClusterPushListener;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubListener;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.cluster.pubsub.api.async.RedisClusterPubSubAsyncCommands;
import io.lettuce.core.cluster.pubsub.api.reactive.RedisClusterPubSubReactiveCommands;
import io.lettuce.core.cluster.pubsub.api.sync.RedisClusterPubSubCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

public class LettuceStatefulRedisClusterPubSubConnection<K, V>
        extends LettuceStatefulConnection<K, V, StatefulRedisClusterPubSubConnection<K, V>, Iterable<RedisURI>>
        implements StatefulRedisClusterPubSubConnection<K, V> {

    private final RedisURI firstUri;

    public LettuceStatefulRedisClusterPubSubConnection(StatefulRedisClusterPubSubConnection<K, V> delegate,
                                                       Iterable<RedisURI> uris,
                                                       DbFailover failover,
                                                       Consumer<DbConnection> closer,
                                                       Function<Iterable<RedisURI>, CompletionStage<StatefulRedisClusterPubSubConnection<K, V>>> recreator) {
        super(delegate, uris, failover, closer, recreator);
        this.firstUri = uris.iterator().next();
    }

    @Override
    public RedisClusterPubSubCommands<K, V> sync() {
        return delegate.sync();
    }

    @Override
    public RedisClusterPubSubAsyncCommands<K, V> async() {
        return delegate.async();
    }

    @Override
    public RedisClusterPubSubReactiveCommands<K, V> reactive() {
        return delegate.reactive();
    }

    @Override
    public StatefulRedisPubSubConnection<K, V> getConnection(String nodeId) {
        return delegate.getConnection(nodeId);
    }

    @Override
    public CompletableFuture<StatefulRedisPubSubConnection<K, V>> getConnectionAsync(String nodeId) {
        return delegate.getConnectionAsync(nodeId);
    }

    @Override
    public StatefulRedisPubSubConnection<K, V> getConnection(String host, int port) {
        return delegate.getConnection(host, port);
    }

    @Override
    public CompletableFuture<StatefulRedisPubSubConnection<K, V>> getConnectionAsync(String host, int port) {
        return delegate.getConnectionAsync(host, port);
    }

    @Override
    public Partitions getPartitions() {
        return delegate.getPartitions();
    }

    @Override
    public void setNodeMessagePropagation(boolean enabled) {
        delegate.setNodeMessagePropagation(enabled);
    }

    @Override
    public void addListener(RedisClusterPubSubListener<K, V> listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(RedisClusterPubSubListener<K, V> listener) {
        delegate.removeListener(listener);
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
    public void addListener(RedisPubSubListener<K, V> listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(RedisPubSubListener<K, V> listener) {
        delegate.removeListener(listener);
    }

    @Override
    public boolean isMulti() {
        return delegate.isMulti();
    }

    @Override
    public void addListener(PushListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(PushListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    protected Iterable<RedisURI> getUri(DbAddress newAddress) {
        return UriUtils.getClusterUris(firstUri, newAddress.getNodes());
    }
}
