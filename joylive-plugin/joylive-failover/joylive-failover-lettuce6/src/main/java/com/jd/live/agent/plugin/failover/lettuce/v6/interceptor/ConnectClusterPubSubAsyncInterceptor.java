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
package com.jd.live.agent.plugin.failover.lettuce.v6.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.failover.lettuce.v6.connection.LettuceStatefulRedisClusterPubSubConnection;
import com.jd.live.agent.plugin.failover.lettuce.v6.context.LettuceContext;
import com.jd.live.agent.plugin.failover.lettuce.v6.util.UriUtils;
import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.codec.RedisCodec;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * ConnectStandaloneAsyncInterceptor
 */
public class ConnectClusterPubSubAsyncInterceptor extends AbstractLettuceInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ConnectClusterPubSubAsyncInterceptor.class);
    private static final String ATTR_URIS = "uris";

    public ConnectClusterPubSubAsyncInterceptor(InvocationContext context) {
        super(context, PRIMARY_ADDRESS_RESOLVER);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        //  for recreate redis connection
        if (LettuceContext.isIgnored()) {
            return;
        }
        Iterable<RedisURI> uris = (Iterable<RedisURI>) Accessor.uris.get(ctx.getTarget());
        RedisURI firstURI = uris.iterator().next();
        String clientName = firstURI.getClientName();
        DbCandidate candidate = getCandidate(TYPE_REDIS, UriUtils.getClusterAddress(uris), getAccessMode(clientName), addressResolver);
        if (candidate.isRedirected()) {
            Accessor.setUris(ctx.getTarget(), UriUtils.getClusterUris(firstURI, candidate.getNewNodes()));
            logger.info("Try reconnecting to {} {}", TYPE_REDIS, candidate.getNewAddress());
        }
        ctx.setAttribute(ATTR_OLD_ADDRESS, candidate);
        ctx.setAttribute(ATTR_URIS, uris);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void onSuccess(ExecutableContext ctx) {
        DbCandidate candidate = ctx.getAttribute(ATTR_OLD_ADDRESS);
        if (candidate == null) {
            return;
        }
        Iterable<RedisURI> uris = ctx.getAttribute(ATTR_URIS);
        ConnectionFuture<StatefulRedisClusterPubSubConnection<?, ?>> future = ((MethodContext) ctx).getResult();
        Function<Iterable<RedisURI>, CompletionStage<?>> recreator = u -> connect(ctx.getTarget(), u, ctx.getArgument(0));
        future.thenAccept(connection -> {
            checkFailover(createConnection(() -> new LettuceStatefulRedisClusterPubSubConnection(connection, uris, toClusterRedirect(candidate), closer, recreator)), addressResolver);
        });
    }

    /**
     * Asynchronously establishes a Redis cluster connection.
     *
     * @param client Redis client configuration
     * @param uris   Cluster node URIs
     * @param codec  Data serialization codec
     * @return Future tracking connection progress
     */
    private CompletionStage<?> connect(Object client, Object uris, Object codec) {
        return LettuceContext.callAsync(() -> (CompletionStage<?>) Accessor.connect(Accessor.setUris(client, uris), codec));
    }

    private static class Accessor {

        private static final UnsafeFieldAccessor uris = UnsafeFieldAccessorFactory.getAccessor(RedisClusterClient.class, "initialUris");

        private static final Method connect = ClassUtils.getDeclaredMethod(RedisClient.class,
                "connectClusterAsync", new Class[]{RedisCodec.class});

        public static Object setUris(Object target, Object value) {
            uris.set(target, value);
            return target;
        }

        public static Object connect(Object target, Object... args) throws InvocationTargetException, IllegalAccessException {
            return connect.invoke(target, args);
        }

    }
}
