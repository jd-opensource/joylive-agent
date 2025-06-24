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
import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.failover.jedis.v5.config.JedisAddress;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObjectInfo;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Set;

public class JedisPoolConnection implements DbConnection {

    private final JedisPool jedisPool;

    private final UnsafeFieldAccessor pooledObject;

    private JedisAddress address;

    public JedisPoolConnection(JedisPool jedisPool, ClusterRedirect address, UnsafeFieldAccessor pooledObject) {
        this.jedisPool = jedisPool;
        this.address = JedisAddress.of(address);
        this.pooledObject = pooledObject;
    }

    @Override
    public ClusterRedirect getAddress() {
        return address.getAddress();
    }

    @Override
    public void close() {
        jedisPool.close();
    }

    @Override
    public boolean isClosed() {
        return jedisPool.isClosed();
    }

    public HostAndPort getHostAndPort() {
        return address;
    }

    @SuppressWarnings("unchecked")
    public ClusterRedirect redirect(ClusterAddress newAddress) {
        // new connection will take the new address.
        this.address = address.newAddress(newAddress);
        // copy
        Set<DefaultPooledObjectInfo> objects = jedisPool.listAllObjects();
        objects.forEach(o -> {
            try {
                PooledObject<Jedis> po = (PooledObject<Jedis>) pooledObject.get(o);
                jedisPool.invalidateObject(po.getObject());
            } catch (Exception ignored) {
                // ignore
            }
        });
        return address.getAddress();
    }
}
