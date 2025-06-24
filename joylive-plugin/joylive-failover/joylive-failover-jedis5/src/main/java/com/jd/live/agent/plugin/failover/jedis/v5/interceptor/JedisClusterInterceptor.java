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
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory;
import com.jd.live.agent.core.util.StringUtils;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.plugin.failover.jedis.v5.config.JedisAddress;
import com.jd.live.agent.plugin.failover.jedis.v5.connection.JedisClusterConnection;
import com.jd.live.agent.plugin.failover.jedis.v5.connection.JedisConnection;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisClusterInfoCache;
import redis.clients.jedis.providers.ClusterConnectionProvider;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * JedisClusterInterceptor
 */
public class JedisClusterInterceptor extends AbstractJedisInterceptor {

    public JedisClusterInterceptor(InvocationContext context) {
        super(context);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected JedisConnection createConnection(ExecutableContext ctx) {
        ClusterConnectionProvider provider = ctx.getArgument(0);
        JedisClusterInfoCache cache = (JedisClusterInfoCache) Accessor.cache.get(provider);
        Set<HostAndPort> startNodes = (Set<HostAndPort>) Accessor.startNodes.get(cache);
        JedisClientConfig clientConfig = (JedisClientConfig) Accessor.clientConfig.get(cache);
        JedisCluster cluster = (JedisCluster) ctx.getTarget();

        List<String> addresses = toList(startNodes, JedisAddress::getAddress);
        AccessMode accessMode = getAccessMode(clientConfig);
        DbCandidate oldCandidate = getCandidate(TYPE_REDIS, StringUtils.join(addresses), addresses.toArray(new String[0]), accessMode, MULTI_ADDRESS_SEMICOLON_RESOLVER);
        return new JedisClusterConnection(cluster, clientConfig, provider, toClusterRedirect(oldCandidate), Accessor.cache, Accessor.initializeSlotsCache);
    }

    private static class Accessor {

        private static final UnsafeFieldAccessor cache = UnsafeFieldAccessorFactory.getAccessor(ClusterConnectionProvider.class, "cache");
        private static final UnsafeFieldAccessor clientConfig = UnsafeFieldAccessorFactory.getAccessor(JedisClusterInfoCache.class, "clientConfig");
        private static final UnsafeFieldAccessor startNodes = UnsafeFieldAccessorFactory.getAccessor(JedisClusterInfoCache.class, "startNodes");
        private static final Method initializeSlotsCache = ClassUtils.getDeclaredMethod(JedisClusterInfoCache.class, "initializeSlotsCache");

    }
}
