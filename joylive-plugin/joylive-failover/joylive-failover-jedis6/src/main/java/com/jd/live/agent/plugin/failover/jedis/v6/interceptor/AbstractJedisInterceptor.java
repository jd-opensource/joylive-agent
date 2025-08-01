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
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory;
import com.jd.live.agent.core.util.Execution;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.governance.interceptor.AbstractDbConnectionInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import com.jd.live.agent.plugin.failover.jedis.v6.connection.JedisConnection;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObjectInfo;
import org.apache.commons.pool2.impl.GenericObjectPool;
import redis.clients.jedis.*;
import redis.clients.jedis.providers.ClusterConnectionProvider;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Function;

import static com.jd.live.agent.core.util.ExceptionUtils.getCause;

/**
 * AbstractJedisInterceptor
 */
public abstract class AbstractJedisInterceptor extends AbstractDbConnectionInterceptor<JedisConnection> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractJedisInterceptor.class);

    protected static final String TYPE_REDIS = "redis";

    protected final Function<LiveDatabase, String> addressResolver;

    public AbstractJedisInterceptor(InvocationContext context, Function<LiveDatabase, String> addressResolver) {
        super(context);
        this.addressResolver = addressResolver;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        checkFailover(createConnection(() -> createConnection(ctx)), addressResolver);
    }

    /**
     * Executes the given operation and logs any errors.
     * Unwraps nested exceptions to get the root cause before logging.
     *
     * @param execution the operation to execute
     */
    protected void executeQuietly(Execution execution) {
        try {
            execution.execute();
        } catch (Throwable e) {
            Throwable cause = getCause(e);
            logger.error(cause.getMessage(), cause);
        }
    }

    /**
     * Creates a new Jedis connection for the given execution context.
     *
     * @param ctx the execution context
     * @return a new Jedis connection instance
     */
    protected abstract JedisConnection createConnection(ExecutableContext ctx);

    // lazy load class to avoid class loading error
    protected static class Accessor {
        protected static final FieldAccessor cache = FieldAccessorFactory.getAccessor(ClusterConnectionProvider.class, "cache");
        protected static final Method initializeSlotsCache = ClassUtils.getDeclaredMethod(ClusterConnectionProvider.class, "initializeSlotsCache");

        protected static final FieldAccessor cacheClientConfig = FieldAccessorFactory.getAccessor(JedisClusterInfoCache.class, "clientConfig");
        protected static final FieldAccessor poolConfig = FieldAccessorFactory.getAccessor(JedisClusterInfoCache.class, "poolConfig");
        protected static final FieldAccessor startNodes = FieldAccessorFactory.getAccessor(JedisClusterInfoCache.class, "startNodes");

        protected static final FieldAccessor socketFactory = FieldAccessorFactory.getAccessor(JedisFactory.class, "jedisSocketFactory");
        protected static final FieldAccessor factoryClientConfig = FieldAccessorFactory.getAccessor(JedisFactory.class, "clientConfig");
        protected static final FieldAccessor hostAndPort = FieldAccessorFactory.getAccessor(DefaultJedisSocketFactory.class, "hostAndPort");
        protected static final FieldAccessor pooledObject = FieldAccessorFactory.getAccessor(DefaultPooledObjectInfo.class, "pooledObject");

        protected static final FieldAccessor masterListeners = FieldAccessorFactory.getAccessor(JedisSentinelPool.class, "masterListeners");
        protected static final Method initSentinels = ClassUtils.getDeclaredMethod(JedisSentinelPool.class, "initSentinels");
        protected static final Method shutdown = ClassUtils.getDeclaredMethod("redis.clients.jedis.JedisSentinelPool$MasterListener", "shutdown");

        protected static final FieldAccessor jedis = FieldAccessorFactory.getAccessor(org.springframework.data.redis.connection.jedis.JedisConnection.class, "jedis");
        protected static final FieldAccessor pool = FieldAccessorFactory.getAccessor(org.springframework.data.redis.connection.jedis.JedisConnection.class, "pool");

        /**
         * Invalidates all Jedis objects in the pool during failover.
         * Silently ignores any errors during invalidation.
         *
         * @param pool the Jedis connection pool to process
         */
        @SuppressWarnings("unchecked")
        protected static void evict(Object pool) {
            // lazy load class to avoid class loading error
            GenericObjectPool<Jedis> objectPool = (GenericObjectPool<Jedis>) pool;
            Set<DefaultPooledObjectInfo> objects = objectPool.listAllObjects();
            objects.forEach(o -> {
                try {
                    PooledObject<Jedis> po = (PooledObject<Jedis>) pooledObject.get(o);
                    objectPool.invalidateObject(po.getObject());
                } catch (Exception ignored) {
                    // ignore
                }
            });
        }

        /**
         * Safely invokes the shutdown method on a listener if it exists.
         * Silently ignores any invocation exceptions.
         *
         * @param listener the listener object to shutdown
         */
        protected static void shutdownListener(Object listener) throws Exception {
            if (shutdown != null && listener != null) {
                shutdown.invoke(listener);
            }
        }

        /**
         * Initializes sentinels on the target object if possible.
         *
         * @param target the target object to initialize (nullable)
         * @param args   the arguments to pass to initialization method
         * @throws Exception if method invocation fails
         */
        protected static void initSentinels(Object target, Object... args) throws Exception {
            if (initSentinels != null && target != null) {
                initSentinels.invoke(target, args);
            }
        }

        /**
         * Initializes sentinels on the target object if possible.
         *
         * @param target the target object to initialize (nullable)
         * @param args   the arguments to pass to initialization method
         * @throws Exception if method invocation fails
         */
        protected static void initializeSlotsCache(Object target, Object... args) throws Exception {
            if (initializeSlotsCache != null && target != null) {
                initializeSlotsCache.invoke(target, args);
            }
        }

    }

}
