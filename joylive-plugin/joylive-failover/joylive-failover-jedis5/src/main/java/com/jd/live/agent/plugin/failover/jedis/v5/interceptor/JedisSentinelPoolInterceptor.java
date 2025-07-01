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
import com.jd.live.agent.governance.db.DbCandidate;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.plugin.failover.jedis.v5.config.JedisAddress;
import com.jd.live.agent.plugin.failover.jedis.v5.connection.JedisConnection;
import com.jd.live.agent.plugin.failover.jedis.v5.connection.JedisSentinelPoolConnection;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisSentinelPool;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.join;

/**
 * JedisSentinelPoolInterceptor
 */
public class JedisSentinelPoolInterceptor extends AbstractJedisInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JedisSentinelPoolInterceptor.class);

    public JedisSentinelPoolInterceptor(InvocationContext context) {
        super(context, MULTI_ADDRESS_SEMICOLON_RESOLVER);
    }

    @Override
    public void onExit(ExecutableContext ctx) {
        ctx.unlock();
    }

    @Override
    protected JedisConnection createConnection(ExecutableContext ctx) {
        JedisSentinelPool pool = (JedisSentinelPool) ctx.getTarget();
        String masterName = ctx.getArgument(0);
        Set<HostAndPort> sentinels = ctx.getArgument(1);
        JedisClientConfig clientConfig = ctx.getArgument(ctx.getArgumentCount() - 1);
        List<String> addresses = toList(sentinels, JedisAddress::getFailover);
        AccessMode accessMode = getAccessMode(clientConfig.getClientName());
        DbCandidate candidate = connectionSupervisor.getCandidate(TYPE_REDIS, join(addresses), addresses.toArray(new String[0]), accessMode, addressResolver);
        if (candidate.isRedirected()) {
            logger.info("Try reconnecting to {} {}", TYPE_REDIS, candidate.getNewAddress());
        }
        return new JedisSentinelPoolConnection(pool, DbFailover.of(candidate), addr -> {
            Collection<?> listeners = (Collection<?>) Accessor.masterListeners.get(pool);
            listeners.forEach(listener -> executeQuietly(() -> Accessor.shutdownListener(listener)));
            listeners.clear();
            executeQuietly(() -> Accessor.initSentinels(pool, JedisAddress.getNodes(addr), masterName));
            Accessor.evict(pool);
        });
    }
}
