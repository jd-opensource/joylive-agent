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
package com.jd.live.agent.plugin.failover.jedis.v3.connection;

import com.jd.live.agent.governance.db.DbAddress;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.governance.db.DbFailoverResponse;
import com.jd.live.agent.plugin.failover.jedis.v3.config.JedisAddress;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPool;

import java.util.function.Consumer;

public class JedisPoolConnection extends AbstractJedisPoolConnection<JedisPool> {

    private JedisAddress address;

    private final Consumer<DbAddress> onFailover;

    public JedisPoolConnection(JedisPool jedisPool, DbFailover failover, Consumer<DbAddress> onFailover) {
        super(jedisPool);
        this.address = JedisAddress.of(failover);
        this.onFailover = onFailover;
    }

    @Override
    public DbFailover getFailover() {
        return address.getFailover();
    }

    public HostAndPort getHostAndPort() {
        return address;
    }

    @Override
    public DbFailoverResponse failover(DbAddress newAddress) {
        // new connection will take the new address.
        this.address = address.newAddress(newAddress);
        onFailover.accept(newAddress);
        return DbFailoverResponse.SUCCESS;
    }
}
