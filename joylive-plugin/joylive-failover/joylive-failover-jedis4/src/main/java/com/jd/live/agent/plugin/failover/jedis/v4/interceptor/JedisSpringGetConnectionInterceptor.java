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
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.failover.jedis.v4.connection.JedisConnection;
import com.jd.live.agent.plugin.failover.jedis.v4.connection.JedisFailoverConnection;
import com.jd.live.agent.plugin.failover.jedis.v4.connection.JedisSpringConnection;
import com.jd.live.agent.plugin.failover.jedis.v4.context.JedisContext;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

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
        JedisSpringConnection result = new JedisSpringConnection((org.springframework.data.redis.connection.jedis.JedisConnection) connection, JedisContext.removeDbFailover(),
                Accessor.jedis, addr -> recreate(connectionFactory), connectionSupervisor::removeConnection);
        mc.setResult(result);
        return result;
    }

    /**
     * Creates a new Jedis connection with failover support.
     *
     * @param factory the connection factory to use
     * @return new failover-enabled connection
     * @throws RuntimeException if connection fails
     */
    private JedisFailoverConnection recreate(JedisConnectionFactory factory) {
        JedisContext.ignore();
        try {
            org.springframework.data.redis.connection.jedis.JedisConnection newConn = (org.springframework.data.redis.connection.jedis.JedisConnection) factory.getConnection();
            return new JedisFailoverConnection(newConn, JedisContext.removeDbFailover());
        } catch (Exception e) {
            logger.error("Failed to create new redis connection.", e);
            throw new RuntimeException(e);
        } finally {
            JedisContext.removeIgnore();
        }
    }

    private static class Accessor {
        private static final UnsafeFieldAccessor jedis = UnsafeFieldAccessorFactory.getAccessor(org.springframework.data.redis.connection.jedis.JedisConnection.class, "jedis");
        private static final UnsafeFieldAccessor pool = UnsafeFieldAccessorFactory.getAccessor(org.springframework.data.redis.connection.jedis.JedisConnection.class, "pool");
    }

}
