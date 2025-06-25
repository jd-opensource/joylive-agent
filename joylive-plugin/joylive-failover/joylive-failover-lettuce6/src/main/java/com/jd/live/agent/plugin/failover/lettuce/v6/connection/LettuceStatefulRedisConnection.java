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
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

public class LettuceStatefulRedisConnection<K, V>
        extends LettuceStatefulConnection<K, V, StatefulRedisConnection<K, V>, RedisURI>
        implements StatefulRedisConnection<K, V> {

    public LettuceStatefulRedisConnection(StatefulRedisConnection<K, V> delegate,
                                          RedisURI uri,
                                          ClusterRedirect address,
                                          Consumer<DbConnection> closer,
                                          Function<RedisURI, CompletionStage<StatefulRedisConnection<K, V>>> recreator) {
        super(delegate, uri, address, closer, recreator);
    }

    @Override
    public boolean isMulti() {
        return delegate.isMulti();
    }

    @Override
    public RedisCommands<K, V> sync() {
        return delegate.sync();
    }

    @Override
    public RedisAsyncCommands<K, V> async() {
        return delegate.async();
    }

    @Override
    public RedisReactiveCommands<K, V> reactive() {
        return delegate.reactive();
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
