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
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.async.RedisSentinelAsyncCommands;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

public class LettuceSentinelRedisConnection<K, V>
        extends LettuceStatefulConnection<K, V, StatefulRedisSentinelConnection<K, V>, RedisURI>
        implements StatefulRedisSentinelConnection<K, V> {

    public LettuceSentinelRedisConnection(StatefulRedisSentinelConnection<K, V> delegate,
                                          RedisURI uri,
                                          DbFailover failover,
                                          Consumer<DbConnection> closer,
                                          Function<RedisURI, CompletionStage<StatefulRedisSentinelConnection<K, V>>> recreator) {
        super(delegate, uri, failover, closer, recreator);
    }

    @Override
    public RedisSentinelCommands<K, V> sync() {
        return delegate.sync();
    }

    @Override
    public RedisSentinelAsyncCommands<K, V> async() {
        return delegate.async();
    }

    @Override
    public RedisSentinelReactiveCommands<K, V> reactive() {
        return delegate.reactive();
    }

    @Override
    protected RedisURI getUri(DbAddress newAddress) {
        return UriUtils.getSentinelUri(uri, newAddress.getNodes());
    }
}
