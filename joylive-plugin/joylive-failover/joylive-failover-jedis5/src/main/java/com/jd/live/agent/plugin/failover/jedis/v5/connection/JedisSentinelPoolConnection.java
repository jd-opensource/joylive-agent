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
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.failover.jedis.v5.config.JedisAddress;
import redis.clients.jedis.JedisSentinelPool;

import java.lang.reflect.Method;
import java.util.Collection;

import static com.jd.live.agent.core.util.ExceptionUtils.getCause;

public class JedisSentinelPoolConnection extends AbstractJedisPoolConnection<JedisSentinelPool> {

    private static final Logger logger = LoggerFactory.getLogger(JedisSentinelPoolConnection.class);

    private ClusterRedirect address;
    private final String masterName;
    private final UnsafeFieldAccessor masterListeners;
    private final Method initSentinels;
    private final Method shutdown;

    public JedisSentinelPoolConnection(JedisSentinelPool sentinelPool,
                                       ClusterRedirect address,
                                       UnsafeFieldAccessor pooledObject,
                                       String masterName,
                                       UnsafeFieldAccessor masterListeners,
                                       Method initSentinels,
                                       Method shutdown) {
        super(sentinelPool, pooledObject);
        this.address = address;
        this.masterName = masterName;
        this.masterListeners = masterListeners;
        this.initSentinels = initSentinels;
        this.shutdown = shutdown;
    }

    @Override
    public ClusterRedirect getAddress() {
        return address;
    }

    @Override
    public ClusterRedirect redirect(ClusterAddress newAddress) {
        this.address = address.newAddress(newAddress);
        Collection<?> listeners = (Collection<?>) masterListeners.get(jedisPool);
        listeners.forEach(this::shutdownListener);
        listeners.clear();
        initSentinels(newAddress);
        evict();
        return address;
    }

    /**
     * Initializes sentinels with the given cluster address.
     * Logs any errors that occur during initialization.
     *
     * @param newAddress the new cluster address to initialize
     */
    private void initSentinels(ClusterAddress newAddress) {
        if (initSentinels != null) {
            try {
                initSentinels.invoke(jedisPool, JedisAddress.getNodes(newAddress), masterName);
            } catch (Throwable e) {
                Throwable cause = getCause(e);
                logger.error(cause.getMessage(), cause);
            }
        }
    }

    /**
     * Shuts down the specified listener.
     * Logs any errors that occur during shutdown.
     *
     * @param listener the listener to shutdown
     */
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

}
