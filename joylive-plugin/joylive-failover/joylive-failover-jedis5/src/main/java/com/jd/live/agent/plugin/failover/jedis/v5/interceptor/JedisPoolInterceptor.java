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
package com.jd.live.agent.plugin.failover.jedis.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory;
import com.jd.live.agent.governance.db.DbCandidate;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.plugin.failover.jedis.v5.config.JedisAddress;
import com.jd.live.agent.plugin.failover.jedis.v5.config.JedisConfig;
import com.jd.live.agent.plugin.failover.jedis.v5.connection.JedisConnection;
import com.jd.live.agent.plugin.failover.jedis.v5.connection.JedisPoolConnection;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObjectInfo;
import redis.clients.jedis.*;

/**
 * JedisPoolInterceptor
 */
public class JedisPoolInterceptor extends AbstractJedisInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JedisPoolInterceptor.class);

    public JedisPoolInterceptor(InvocationContext context) {
        super(context, PRIMARY_ADDRESS_RESOLVER);
    }

    @Override
    protected JedisConnection createConnection(ExecutableContext ctx) {
        // change HostAndPortMapper of jedis client config in constructor
        PooledObjectFactory<Jedis> factory = ctx.getArgument(1);
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
        JedisPoolConnection connection = new JedisPoolConnection((JedisPool) ctx.getTarget(), DbFailover.of(candidate), Accessor.pooledObject);
        JedisConfig config = new JedisConfig(clientConfig, hp -> connection.getHostAndPort());
        Accessor.config.set(factory, config);
        if (candidate.isRedirected()) {
            logger.info("Try reconnecting to {} {}", TYPE_REDIS, candidate.getNewAddress());
        }
        return connection;
    }

    private static class Accessor {

        private static final UnsafeFieldAccessor config = UnsafeFieldAccessorFactory.getAccessor(JedisFactory.class, "clientConfig");
        private static final UnsafeFieldAccessor socketFactory = UnsafeFieldAccessorFactory.getAccessor(JedisFactory.class, "jedisSocketFactory");
        private static final UnsafeFieldAccessor hostAndPort = UnsafeFieldAccessorFactory.getAccessor(DefaultJedisSocketFactory.class, "hostAndPort");
        private static final UnsafeFieldAccessor pooledObject = UnsafeFieldAccessorFactory.getAccessor(DefaultPooledObjectInfo.class, "pooledObject");

    }

}
