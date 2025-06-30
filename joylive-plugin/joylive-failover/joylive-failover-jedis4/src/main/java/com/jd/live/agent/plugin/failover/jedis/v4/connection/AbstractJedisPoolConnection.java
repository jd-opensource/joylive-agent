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
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObjectInfo;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.Pool;

import java.util.Set;

/**
 * Abstract base class for Jedis pool connections.
 *
 * @param <T> the type of Jedis pool implementation
 */
public abstract class AbstractJedisPoolConnection<T extends Pool<Jedis>> implements JedisConnection {

    protected final T jedisPool;
    protected final UnsafeFieldAccessor pooledObject;

    public AbstractJedisPoolConnection(T jedisPool, UnsafeFieldAccessor pooledObject) {
        this.jedisPool = jedisPool;
        this.pooledObject = pooledObject;
    }

    @Override
    public void close() {
        jedisPool.close();
    }

    @Override
    public boolean isClosed() {
        return jedisPool.isClosed();
    }

    /**
     * Evicts all objects from the sentinel pool.
     * Logs any errors that occur during eviction.
     */
    @SuppressWarnings("unchecked")
    protected void evict() {
        Set<DefaultPooledObjectInfo> objects = jedisPool.listAllObjects();
        objects.forEach(o -> {
            try {
                PooledObject<Jedis> po = (PooledObject<Jedis>) pooledObject.get(o);
                jedisPool.invalidateObject(po.getObject());
            } catch (Exception ignored) {
                // ignore
            }
        });
    }
}
