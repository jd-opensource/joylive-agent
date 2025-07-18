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
package com.jd.live.agent.plugin.failover.jedis.v6.connection;

import com.jd.live.agent.governance.db.DbAddress;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.governance.db.DbFailoverResponse;
import redis.clients.jedis.JedisSentinelPool;

import java.util.function.Consumer;

public class JedisSentinelPoolConnection extends AbstractJedisPoolConnection<JedisSentinelPool> {

    private DbFailover failover;

    private final Consumer<DbAddress> onFailover;

    public JedisSentinelPoolConnection(JedisSentinelPool sentinelPool, DbFailover failover, Consumer<DbAddress> onFailover) {
        super(sentinelPool);
        this.failover = failover;
        this.onFailover = onFailover;
    }

    @Override
    public DbFailover getFailover() {
        return failover;
    }

    @Override
    public DbFailoverResponse failover(DbAddress newAddress) {
        this.failover = failover.newAddress(newAddress);
        onFailover.accept(newAddress);
        return DbFailoverResponse.SUCCESS;
    }
}
