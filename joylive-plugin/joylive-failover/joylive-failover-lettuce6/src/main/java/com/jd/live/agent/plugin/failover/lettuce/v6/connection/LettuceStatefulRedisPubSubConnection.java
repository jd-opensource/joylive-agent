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
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

public class LettuceStatefulRedisPubSubConnection<K, V>
        extends LettuceStatefulConnection<K, V, StatefulRedisPubSubConnection<K, V>, RedisURI>
        implements StatefulRedisPubSubConnection<K, V> {

    public LettuceStatefulRedisPubSubConnection(StatefulRedisPubSubConnection<K, V> delegate,
                                                RedisURI uri,
                                                ClusterRedirect address,
                                                Consumer<DbConnection> closer,
                                                Function<RedisURI, CompletionStage<StatefulRedisPubSubConnection<K, V>>> recreator) {
        super(delegate, uri, address, closer, recreator);
    }

    @Override
    public RedisPubSubCommands<K, V> sync() {
        return delegate.sync();
    }

    @Override
    public RedisPubSubAsyncCommands<K, V> async() {
        return delegate.async();
    }

    @Override
    public RedisPubSubReactiveCommands<K, V> reactive() {
        return delegate.reactive();
    }

    @Override
    public boolean isMulti() {
        return delegate.isMulti();
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
    public void addListener(PushListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(PushListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    protected RedisURI getUri(ClusterAddress newAddress) {
        return UriUtils.getUri(uri, newAddress.getAddress());
    }
}
