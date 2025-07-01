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
package com.jd.live.agent.plugin.failover.jedis.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.db.DbCandidate;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.plugin.failover.jedis.v4.config.JedisAddress;
import com.jd.live.agent.plugin.failover.jedis.v4.connection.JedisClusterConnection;
import com.jd.live.agent.plugin.failover.jedis.v4.connection.JedisConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;
import redis.clients.jedis.providers.ConnectionProvider;

import java.util.List;
import java.util.Set;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.join;

/**
 * JedisClusterInterceptor
 */
public class JedisClusterInterceptor extends AbstractJedisInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JedisClusterInterceptor.class);

    public JedisClusterInterceptor(InvocationContext context) {
        super(context, MULTI_ADDRESS_SEMICOLON_RESOLVER);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected JedisConnection createConnection(ExecutableContext ctx) {
        JedisCluster cluster = (JedisCluster) ctx.getTarget();
        ConnectionProvider provider = (ConnectionProvider) Accessor.provider.get(cluster);
        JedisClusterInfoCache cache = (JedisClusterInfoCache) Accessor.cache.get(provider);
        JedisClientConfig clientConfig = (JedisClientConfig) Accessor.cacheClientConfig.get(cache);
        GenericObjectPoolConfig<Connection> poolConfig = (GenericObjectPoolConfig<Connection>) Accessor.poolConfig.get(cache);
        Set<HostAndPort> startNodes = (Set<HostAndPort>) Accessor.startNodes.get(cache);

        List<String> addresses = toList(startNodes, JedisAddress::getFailover);
        AccessMode accessMode = getAccessMode(clientConfig.getClientName());
        DbCandidate candidate = connectionSupervisor.getCandidate(TYPE_REDIS, join(addresses), addresses.toArray(new String[0]), accessMode, addressResolver);
        if (candidate.isRedirected()) {
            logger.info("Try reconnecting to {} {}", TYPE_REDIS, candidate.getNewAddress());
        }
        return new JedisClusterConnection(cluster, DbFailover.of(candidate), addr -> {
            // reget cache after failover
            JedisClusterInfoCache oldCache = (JedisClusterInfoCache) Accessor.cache.get(provider);
            JedisClusterInfoCache newCache = new JedisClusterInfoCache(clientConfig, poolConfig, JedisAddress.getNodes(addr));
            Accessor.cache.set(provider, newCache);
            executeQuietly(() -> Accessor.initializeSlotsCache.invoke(provider, JedisAddress.getNodes(addr), clientConfig));
            oldCache.reset();
        });
    }
}
