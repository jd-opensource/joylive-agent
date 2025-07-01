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
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.failover.jedis.v6.connection.JedisConnection;
import com.jd.live.agent.plugin.failover.jedis.v6.connection.JedisUnpoolConnection;
import com.jd.live.agent.plugin.failover.jedis.v6.connection.JedisUnpoolSpringConnection;
import com.jd.live.agent.plugin.failover.jedis.v6.context.JedisContext;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.Jedis;

/**
 * JedisConnectionFactoryInterceptor
 */
public class JedisSpringGetConnectionInterceptor extends AbstractJedisInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JedisSpringGetConnectionInterceptor.class);

    public JedisSpringGetConnectionInterceptor(InvocationContext context) {
        super(context, PRIMARY_ADDRESS_RESOLVER);
    }

    @Override
    protected JedisConnection createConnection(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        JedisConnectionFactory connectionFactory = (JedisConnectionFactory) mc.getTarget();
        RedisConnection connection = mc.getResult();
        if (connection instanceof RedisClusterConnection) {
            // cluster connection is handled by JedisClusterInterceptor.
            return null;
        } else if (Accessor.pool.get(connection) != null) {
            // pooled jedis connection is handled by JedisPoolInterceptor.
            return null;
        } else if (JedisContext.isIgnored()) {
            //  for recreate redis connection
            return null;
        }
        JedisUnpoolSpringConnection result = new JedisUnpoolSpringConnection(connection, JedisContext.removeDbFailover(),
                connectionSupervisor::removeConnection, addr -> {
            JedisContext.ignore();
            try {
                RedisConnection newConn = connectionFactory.getConnection();
                Jedis jedis = ((org.springframework.data.redis.connection.jedis.JedisConnection) newConn).getJedis();
                Accessor.jedis.set(connection, jedis);
                return new JedisUnpoolConnection(newConn, JedisContext.removeDbFailover());
            } catch (Exception e) {
                logger.error("Failed to create new redis connection.", e);
                throw new RuntimeException(e);
            } finally {
                JedisContext.removeIgnore();
            }
        });
        mc.setResult(result);
        return result;
    }

}
