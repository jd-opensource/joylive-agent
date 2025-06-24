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
import com.jd.live.agent.core.util.network.Address;
import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.interceptor.AbstractDbConnectionInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import com.jd.live.agent.plugin.failover.jedis.v5.config.JedisConfig;
import com.jd.live.agent.plugin.failover.jedis.v5.connection.JedisPoolConnection;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObjectInfo;
import redis.clients.jedis.*;

/**
 * JedisPoolInterceptor
 */
public class JedisPoolInterceptor extends AbstractDbConnectionInterceptor<DbConnection> {

    public JedisPoolInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        // change HostAndPortMapper of jedis client config in constructor
        PooledObjectFactory<Jedis> factory = ctx.getArgument(1);
        if (!(factory instanceof JedisFactory)) {
            return;
        }
        JedisClientConfig clientConfig = (JedisClientConfig) Accessor.config.get(factory);
        JedisSocketFactory socketFactory = (JedisSocketFactory) Accessor.socketFactory.get(factory);
        if (!(socketFactory instanceof DefaultJedisSocketFactory)) {
            return;
        }

        HostAndPort hostAndPort = (HostAndPort) Accessor.hostAndPort.get(socketFactory);
        Address address = Address.parse(hostAndPort.getHost(), hostAndPort.getPort());
        AccessMode accessMode = getAccessMode(clientConfig.getClientName(), null, null);
        DbCandidate oldCandidate = getCandidate("redis", address.getAddress(), accessMode, PRIMARY_ADDRESS_RESOLVER);
        JedisPoolConnection connection = new JedisPoolConnection((JedisPool) ctx.getTarget(), toClusterRedirect(oldCandidate), Accessor.pooledObject);
        JedisConfig config = new JedisConfig(clientConfig, hp -> connection.getHostAndPort());
        Accessor.config.set(factory, config);
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
        if (connection instanceof JedisPoolConnection) {
            ClusterRedirect.redirect(((JedisPoolConnection) connection).redirect(address), consumer);
        }
    }

    private static class Accessor {

        private static final UnsafeFieldAccessor config = UnsafeFieldAccessorFactory.getAccessor(JedisFactory.class, "clientConfig");

        private static final UnsafeFieldAccessor socketFactory = UnsafeFieldAccessorFactory.getAccessor(JedisFactory.class, "jedisSocketFactory");

        private static final UnsafeFieldAccessor hostAndPort = UnsafeFieldAccessorFactory.getAccessor(DefaultJedisSocketFactory.class, "hostAndPort");

        private final static UnsafeFieldAccessor pooledObject = UnsafeFieldAccessorFactory.getAccessor(DefaultPooledObjectInfo.class, "pooledObject");

    }

}
