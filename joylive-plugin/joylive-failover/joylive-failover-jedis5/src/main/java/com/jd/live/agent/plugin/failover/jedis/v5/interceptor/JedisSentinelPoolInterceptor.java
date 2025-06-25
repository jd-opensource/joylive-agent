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
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.plugin.failover.jedis.v5.config.JedisAddress;
import com.jd.live.agent.plugin.failover.jedis.v5.connection.JedisConnection;
import com.jd.live.agent.plugin.failover.jedis.v5.connection.JedisSentinelPoolConnection;
import org.apache.commons.pool2.impl.DefaultPooledObjectInfo;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisSentinelPool;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.join;

/**
 * JedisClusterInterceptor
 */
public class JedisSentinelPoolInterceptor extends AbstractJedisInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JedisSentinelPoolInterceptor.class);

    public JedisSentinelPoolInterceptor(InvocationContext context) {
        super(context, MULTI_ADDRESS_SEMICOLON_RESOLVER);
    }

    @Override
    protected JedisConnection createConnection(ExecutableContext ctx) {
        String masterName = ctx.getArgument(0);
        Set<HostAndPort> sentinels = ctx.getArgument(1);
        JedisSentinelPool sentinelPool = (JedisSentinelPool) ctx.getTarget();
        JedisClientConfig clientConfig = ctx.getArgument(ctx.getArgumentCount() - 1);
        List<String> addresses = toList(sentinels, JedisAddress::getAddress);
        AccessMode accessMode = getAccessMode(clientConfig);
        DbCandidate candidate = getCandidate(TYPE_REDIS, join(addresses), addresses.toArray(new String[0]), accessMode, addressResolver);
        if (candidate.isRedirected()) {
            logger.info("Try reconnecting to {} {}", TYPE_REDIS, candidate.getNewAddress());
        }
        return new JedisSentinelPoolConnection(sentinelPool, toClusterRedirect(candidate), Accessor.pooledObject, masterName,
                Accessor.masterListeners, Accessor.initSentinels, Accessor.shutdown);
    }

    private static class Accessor {

        private static final UnsafeFieldAccessor masterListeners = UnsafeFieldAccessorFactory.getAccessor(JedisSentinelPool.class, "masterListeners");
        private static final UnsafeFieldAccessor pooledObject = UnsafeFieldAccessorFactory.getAccessor(DefaultPooledObjectInfo.class, "pooledObject");
        private static final Method initSentinels = ClassUtils.getDeclaredMethod(JedisSentinelPool.class, "initSentinels");
        private static final Method shutdown = ClassUtils.getDeclaredMethod("redis.clients.jedis.JedisSentinelPool$MasterListener", "shutdown");

    }
}
