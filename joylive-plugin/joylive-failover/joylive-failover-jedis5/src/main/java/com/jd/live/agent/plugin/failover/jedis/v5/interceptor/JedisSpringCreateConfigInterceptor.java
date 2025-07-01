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
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.network.Address;
import com.jd.live.agent.governance.db.DbAddress;
import com.jd.live.agent.governance.db.DbCandidate;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.governance.interceptor.AbstractDbFailoverInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.failover.jedis.v5.config.JedisAddress;
import com.jd.live.agent.plugin.failover.jedis.v5.config.JedisConfig;
import com.jd.live.agent.plugin.failover.jedis.v5.context.JedisContext;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.JedisClientConfig;

import static com.jd.live.agent.plugin.failover.jedis.v5.interceptor.AbstractJedisInterceptor.TYPE_REDIS;

/**
 * JedisSpringCreateConfigInterceptor
 */
public class JedisSpringCreateConfigInterceptor extends AbstractDbFailoverInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JedisSpringCreateConfigInterceptor.class);

    public JedisSpringCreateConfigInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        JedisConnectionFactory factory = (JedisConnectionFactory) mc.getTarget();
        if (factory.getUsePool() || factory.isRedisClusterAware()) {
            // only for standalone client
            return;
        }
        JedisClientConfig config = mc.getResult();
        mc.setResult(new JedisConfig(config, hp -> {
            // connect to new address
            DbAddress newAddress = new DbAddress(TYPE_REDIS, Address.parse(hp.getHost(), hp.getPort()).getAddress());
            DbCandidate candidate = connectionSupervisor.getCandidate(newAddress, getAccessMode(config.getClientName()), PRIMARY_ADDRESS_RESOLVER);
            DbFailover dbFailover = DbFailover.of(candidate);
            if (candidate.isRedirected()) {
                DbAddress oldAddress = connectionSupervisor.getFailover(newAddress);
                if (!dbFailover.getNewAddress().equals(oldAddress)) {
                    logger.info("Try reconnecting to {} {}", TYPE_REDIS, candidate.getNewAddress());
                }
            }
            JedisContext.setDbFailover(dbFailover);
            return JedisAddress.of(dbFailover);
        }));
    }
}
