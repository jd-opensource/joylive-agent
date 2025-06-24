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
import com.jd.live.agent.core.util.network.Address;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.interceptor.AbstractDbConnectionInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.failover.jedis.v5.connection.JedisClusterConnection;
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
public class JedisClusterInterceptor extends AbstractDbConnectionInterceptor<DbConnection> {

    public JedisClusterInterceptor(InvocationContext context) {
        super(context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ExecutableContext ctx) {
        ClusterConnectionProvider provider = ctx.getArgument(0);
        JedisClusterInfoCache cache = (JedisClusterInfoCache) Accessor.cache.get(provider);
        Set<HostAndPort> startNodes = (Set<HostAndPort>) Accessor.startNodes.get(cache);
        JedisClientConfig clientConfig = (JedisClientConfig) Accessor.clientConfig.get(cache);
        JedisCluster cluster = (JedisCluster) ctx.getTarget();

        List<String> addresses = toList(startNodes, hp -> Address.parse(hp.getHost(), hp.getPort()).toString());
        AccessMode accessMode = getAccessMode(clientConfig.getClientName(), null, null);
        DbCandidate oldCandidate = getCandidate("redis", StringUtils.join(addresses), addresses.toArray(new String[0]), accessMode, PRIMARY_ADDRESS_RESOLVER);
        JedisClusterConnection connection = new JedisClusterConnection(cluster, clientConfig, provider, toClusterRedirect(oldCandidate), Accessor.cache, Accessor.initializeSlotsCache);
        addConnection(connection);

        ClusterRedirect.redirect(connection.getAddress(), oldCandidate.isRedirected() ? consumer : null);
        // Avoid missing events caused by synchronous changes
        DbCandidate newCandidate = getCandidate(connection.getAddress(), PRIMARY_ADDRESS_RESOLVER);
        if (isChanged(oldCandidate, newCandidate)) {
            publisher.offer(new DatabaseEvent(this));
        }
    }

    @Override
    protected void redirectTo(DbConnection connection, ClusterAddress address) {
        if (connection instanceof JedisClusterConnection) {
            ClusterRedirect.redirect(((JedisClusterConnection) connection).redirect(address), consumer);
        }
    }

    private static class Accessor {

        private static final UnsafeFieldAccessor cache = UnsafeFieldAccessorFactory.getAccessor(ClusterConnectionProvider.class, "cache");

        private static final UnsafeFieldAccessor clientConfig = UnsafeFieldAccessorFactory.getAccessor(JedisClusterInfoCache.class, "clientConfig");

        private static final UnsafeFieldAccessor startNodes = UnsafeFieldAccessorFactory.getAccessor(JedisClusterInfoCache.class, "startNodes");

        private static final Method initializeSlotsCache = ClassUtils.getDeclaredMethod(JedisClusterInfoCache.class, "initializeSlotsCache");

    }
}
