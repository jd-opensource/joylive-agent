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
package com.jd.live.agent.plugin.failover.jedis.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory;
import com.jd.live.agent.governance.db.DbCandidate;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.plugin.failover.jedis.v3.config.JedisAddress;
import com.jd.live.agent.plugin.failover.jedis.v3.config.JedisConfig;
import com.jd.live.agent.plugin.failover.jedis.v3.connection.JedisConnection;
import com.jd.live.agent.plugin.failover.jedis.v3.connection.JedisPoolConnection;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObjectInfo;
import org.apache.commons.pool2.impl.GenericObjectPool;
import redis.clients.jedis.*;

/**
 * JedisPoolInterceptor
 */
public class JedisPoolInterceptor extends AbstractJedisInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JedisPoolInterceptor.class);

    public JedisPoolInterceptor(InvocationContext context) {
        super(context, PRIMARY_ADDRESS_RESOLVER);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected JedisConnection createConnection(ExecutableContext ctx) {
        JedisPool jedisPool = (JedisPool) ctx.getTarget();
        GenericObjectPool<Jedis> internalPool = (GenericObjectPool<Jedis>) Accessor.internalPool.get(jedisPool);
        PooledObjectFactory<Jedis> factory = (PooledObjectFactory<Jedis>) Accessor.factory.get(internalPool);
        if (!(factory instanceof JedisFactory)) {
            return null;
        }
        JedisClientConfig clientConfig = (JedisClientConfig) Accessor.config.get(factory);
        JedisSocketFactory socketFactory = (JedisSocketFactory) Accessor.socketFactory.get(factory);
        if (!(socketFactory instanceof DefaultJedisSocketFactory)) {
            return null;
        }

        HostAndPort hostAndPort = (HostAndPort) Accessor.hostAndPort.get(socketFactory);
        AccessMode accessMode = getAccessMode(clientConfig);
        DbCandidate candidate = connectionSupervisor.getCandidate(TYPE_REDIS, JedisAddress.getFailover(hostAndPort), accessMode, addressResolver);
        JedisPoolConnection connection = new JedisPoolConnection(jedisPool, DbFailover.of(candidate), internalPool, Accessor.pooledObject);
        JedisConfig config = new JedisConfig(clientConfig, hp -> connection.getHostAndPort());
        Accessor.config.set(factory, config);
        if (candidate.isRedirected()) {
            logger.info("Try reconnecting to {} {}", TYPE_REDIS, candidate.getNewAddress());
        }
        return connection;
    }

    private static class Accessor {

        private static final UnsafeFieldAccessor internalPool = UnsafeFieldAccessorFactory.getAccessor(JedisPool.class, "internalPool");
        private static final UnsafeFieldAccessor factory = UnsafeFieldAccessorFactory.getAccessor(GenericObjectPool.class, "factory");
        private static final UnsafeFieldAccessor socketFactory = UnsafeFieldAccessorFactory.getAccessor(JedisFactory.class, "jedisSocketFactory");
        private static final UnsafeFieldAccessor config = UnsafeFieldAccessorFactory.getAccessor(JedisFactory.class, "clientConfig");
        private static final UnsafeFieldAccessor hostAndPort = UnsafeFieldAccessorFactory.getAccessor(DefaultJedisSocketFactory.class, "hostAndPort");
        private static final UnsafeFieldAccessor pooledObject = UnsafeFieldAccessorFactory.getAccessor(DefaultPooledObjectInfo.class, "pooledObject");

    }

}
