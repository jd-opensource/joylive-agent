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
import com.jd.live.agent.plugin.failover.jedis.v5.connection.JedisSentinelPoolConnection;
import org.apache.commons.pool2.impl.DefaultPooledObjectInfo;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisSentinelPool;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * JedisClusterInterceptor
 */
public class JedisSentinelPoolInterceptor extends AbstractDbConnectionInterceptor<DbConnection> {

    public JedisSentinelPoolInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        String masterName = ctx.getArgument(0);
        Set<HostAndPort> sentinels = ctx.getArgument(1);
        JedisSentinelPool sentinelPool = (JedisSentinelPool) ctx.getTarget();
        JedisClientConfig clientConfig = ctx.getArgument(ctx.getArgumentCount() - 1);
        List<String> addresses = toList(sentinels, hp -> Address.parse(hp.getHost(), hp.getPort()).toString());
        AccessMode accessMode = getAccessMode(clientConfig.getClientName(), null, null);
        DbCandidate oldCandidate = getCandidate("redis", StringUtils.join(addresses), addresses.toArray(new String[0]), accessMode, PRIMARY_ADDRESS_RESOLVER);
        JedisSentinelPoolConnection connection = new JedisSentinelPoolConnection(sentinelPool, masterName, toClusterRedirect(oldCandidate),
                Accessor.masterListeners, Accessor.pooledObject, Accessor.initSentinels, Accessor.shutdown);
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
        if (connection instanceof JedisSentinelPoolConnection) {
            ClusterRedirect.redirect(((JedisSentinelPoolConnection) connection).redirect(address), consumer);
        }
    }

    private static class Accessor {

        private static final UnsafeFieldAccessor masterListeners = UnsafeFieldAccessorFactory.getAccessor(JedisSentinelPool.class, "masterListeners");

        private final static UnsafeFieldAccessor pooledObject = UnsafeFieldAccessorFactory.getAccessor(DefaultPooledObjectInfo.class, "pooledObject");

        private static final Method initSentinels = ClassUtils.getDeclaredMethod(JedisSentinelPool.class, "initSentinels");

        private static final Method shutdown = ClassUtils.getDeclaredMethod("redis.clients.jedis.JedisSentinelPool$MasterListener", "shutdown");

    }
}
