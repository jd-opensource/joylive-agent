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
package com.jd.live.agent.plugin.failover.jedis.v4.connection;

import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.governance.db.DbAddress;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.governance.db.DbFailoverResponse;
import com.jd.live.agent.plugin.failover.jedis.v4.config.JedisAddress;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPool;

public class JedisPoolConnection extends AbstractJedisPoolConnection<JedisPool> {

    private JedisAddress address;

    public JedisPoolConnection(JedisPool jedisPool, DbFailover failover, UnsafeFieldAccessor pooledObject) {
        super(jedisPool, pooledObject);
        this.address = JedisAddress.of(failover);
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
        // copy
        evict();
        return DbFailoverResponse.SUCCESS;
    }
}
