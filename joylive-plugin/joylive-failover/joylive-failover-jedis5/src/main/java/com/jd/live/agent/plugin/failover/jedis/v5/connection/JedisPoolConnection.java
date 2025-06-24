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
package com.jd.live.agent.plugin.failover.jedis.v5.connection;

import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.failover.jedis.v5.config.JedisAddress;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPool;

public class JedisPoolConnection extends AbstractJedisPoolConnection<JedisPool> {

    private JedisAddress address;

    public JedisPoolConnection(JedisPool jedisPool, ClusterRedirect address, UnsafeFieldAccessor pooledObject) {
        super(jedisPool, pooledObject);
        this.address = JedisAddress.of(address);
    }

    @Override
    public ClusterRedirect getAddress() {
        return address.getAddress();
    }

    public HostAndPort getHostAndPort() {
        return address;
    }

    @Override
    public ClusterRedirect redirect(ClusterAddress newAddress) {
        // new connection will take the new address.
        this.address = address.newAddress(newAddress);
        // copy
        evict();
        return address.getAddress();
    }
}
