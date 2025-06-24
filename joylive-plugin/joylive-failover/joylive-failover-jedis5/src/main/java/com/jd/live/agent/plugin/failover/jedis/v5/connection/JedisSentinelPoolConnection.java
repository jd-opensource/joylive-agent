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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.failover.jedis.v5.config.JedisAddress;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObjectInfo;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import static com.jd.live.agent.core.util.ExceptionUtils.getCause;

public class JedisSentinelPoolConnection implements DbConnection {

    private static final Logger logger = LoggerFactory.getLogger(JedisSentinelPoolConnection.class);

    private final JedisSentinelPool sentinelPool;

    private final String masterName;

    private final UnsafeFieldAccessor masterListeners;

    private final UnsafeFieldAccessor pooledObject;

    private final Method initSentinels;

    private final Method shutdown;

    private ClusterRedirect address;

    public JedisSentinelPoolConnection(JedisSentinelPool sentinelPool,
                                       String masterName,
                                       ClusterRedirect address,
                                       UnsafeFieldAccessor masterListeners,
                                       UnsafeFieldAccessor pooledObject,
                                       Method initSentinels,
                                       Method shutdown) {
        this.sentinelPool = sentinelPool;
        this.masterName = masterName;
        this.address = address;
        this.pooledObject = pooledObject;
        this.masterListeners = masterListeners;
        this.initSentinels = initSentinels;
        this.shutdown = shutdown;
    }

    @Override
    public ClusterRedirect getAddress() {
        return address;
    }

    @Override
    public void close() {
        sentinelPool.close();
    }

    @Override
    public boolean isClosed() {
        return sentinelPool.isClosed();
    }

    public ClusterRedirect redirect(ClusterAddress newAddress) {
        this.address = address.newAddress(newAddress);
        Collection<?> listeners = (Collection<?>) masterListeners.get(sentinelPool);
        listeners.forEach(this::shutdownListener);
        listeners.clear();
        initSentinels(newAddress);
        evict();
        return address;
    }

    private void initSentinels(ClusterAddress newAddress) {
        if (initSentinels != null) {
            try {
                initSentinels.invoke(sentinelPool, JedisAddress.getNodes(newAddress), masterName);
            } catch (Throwable e) {
                Throwable cause = getCause(e);
                logger.error(cause.getMessage(), cause);
            }
        }
    }

    private void shutdownListener(Object listener) {
        if (shutdown != null) {
            try {
                shutdown.invoke(listener);
            } catch (Throwable e) {
                Throwable cause = getCause(e);
                logger.error(cause.getMessage(), cause);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void evict() {
        Set<DefaultPooledObjectInfo> objects = sentinelPool.listAllObjects();
        objects.forEach(o -> {
            try {
                PooledObject<Jedis> po = (PooledObject<Jedis>) pooledObject.get(o);
                sentinelPool.invalidateObject(po.getObject());
            } catch (Exception e) {
                Throwable cause = getCause(e);
                logger.error(cause.getMessage(), cause);
            }
        });
    }
}
