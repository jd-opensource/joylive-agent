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
package com.jd.live.agent.plugin.failover.jedis.v6.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.db.DbCandidate;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.plugin.failover.jedis.v6.config.JedisAddress;
import com.jd.live.agent.plugin.failover.jedis.v6.config.JedisConfig;
import com.jd.live.agent.plugin.failover.jedis.v6.connection.JedisConnection;
import com.jd.live.agent.plugin.failover.jedis.v6.connection.JedisPoolConnection;
import org.apache.commons.pool2.PooledObjectFactory;
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
        JedisPool jedisPool = (JedisPool) ctx.getTarget();
        PooledObjectFactory<Jedis> factory = ctx.getArgument(ctx.getArgumentCount() - 1);
        if (!(factory instanceof JedisFactory)) {
            return null;
        }
        JedisClientConfig clientConfig = (JedisClientConfig) Accessor.factoryClientConfig.get(factory);
        JedisSocketFactory socketFactory = (JedisSocketFactory) Accessor.socketFactory.get(factory);
        if (!(socketFactory instanceof DefaultJedisSocketFactory)) {
            return null;
        }

        HostAndPort hostAndPort = (HostAndPort) Accessor.hostAndPort.get(socketFactory);
        AccessMode accessMode = getAccessMode(clientConfig.getClientName());
        DbCandidate candidate = connectionSupervisor.getCandidate(TYPE_REDIS, JedisAddress.getFailover(hostAndPort), accessMode, addressResolver);
        JedisPoolConnection connection = new JedisPoolConnection(jedisPool, DbFailover.of(candidate), addr -> Accessor.evict(jedisPool));
        JedisConfig config = new JedisConfig(clientConfig, hp -> connection.getHostAndPort());
        Accessor.factoryClientConfig.set(factory, config);
        if (candidate.isRedirected()) {
            logger.info("Try reconnecting to {} {}", TYPE_REDIS, candidate.getNewAddress());
        }
        return connection;
    }

}
