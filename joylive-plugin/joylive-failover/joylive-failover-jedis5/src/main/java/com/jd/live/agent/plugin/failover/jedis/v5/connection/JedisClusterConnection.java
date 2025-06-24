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
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisClusterInfoCache;
import redis.clients.jedis.providers.ClusterConnectionProvider;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jd.live.agent.core.util.ExceptionUtils.getCause;

public class JedisClusterConnection implements DbConnection {

    private static final Logger logger = LoggerFactory.getLogger(JedisClusterConnection.class);

    private final JedisCluster jedisCluster;

    private final JedisClientConfig clientConfig;

    private final ClusterConnectionProvider provider;

    private final UnsafeFieldAccessor cache;

    private final Method initializeSlotsCache;

    private ClusterRedirect address;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    public JedisClusterConnection(JedisCluster jedisCluster,
                                  JedisClientConfig clientConfig,
                                  ClusterConnectionProvider provider,
                                  ClusterRedirect address,
                                  UnsafeFieldAccessor cache,
                                  Method initializeSlotsCache) {
        this.jedisCluster = jedisCluster;
        this.clientConfig = clientConfig;
        this.provider = provider;
        this.address = address;
        this.cache = cache;
        this.initializeSlotsCache = initializeSlotsCache;
    }

    @Override
    public ClusterRedirect getAddress() {
        return address;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            jedisCluster.close();
        }
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    public ClusterRedirect redirect(ClusterAddress newAddress) {
        this.address = address.newAddress(newAddress);
        JedisClusterInfoCache jc = (JedisClusterInfoCache) cache.get(provider);
        cache.set(provider, new JedisClusterInfoCache(clientConfig, JedisAddress.getNodes(newAddress)));
        if (initializeSlotsCache != null) {
            try {
                initializeSlotsCache.invoke(provider);
            } catch (Throwable e) {
                Throwable cause = getCause(e);
                logger.error(cause.getMessage(), cause);
            }
        }
        jc.close();
        return address;
    }
}
