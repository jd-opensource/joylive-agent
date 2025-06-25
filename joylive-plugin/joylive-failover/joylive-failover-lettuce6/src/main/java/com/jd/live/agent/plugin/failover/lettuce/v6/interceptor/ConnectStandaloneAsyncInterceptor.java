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
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.failover.lettuce.v6.connection.LettuceStatefulRedisConnection;
import com.jd.live.agent.plugin.failover.lettuce.v6.context.LettuceContext;
import com.jd.live.agent.plugin.failover.lettuce.v6.util.UriUtils;
import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * ConnectStandaloneAsyncInterceptor
 */
public class ConnectStandaloneAsyncInterceptor extends AbstractLettuceInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ConnectStandaloneAsyncInterceptor.class);

    public ConnectStandaloneAsyncInterceptor(InvocationContext context) {
        super(context, PRIMARY_ADDRESS_RESOLVER);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        //  for recreate redis connection
        if (LettuceContext.isIgnored()) {
            return;
        }
        RedisURI uri = ctx.getArgument(1);
        DbCandidate candidate = getCandidate(TYPE_REDIS, UriUtils.getAddress(uri), getAccessMode(uri.getClientName()), addressResolver);
        if (candidate.isRedirected()) {
            ctx.setArgument(1, UriUtils.getUri(uri, candidate.getNewAddress()));
            logger.info("Try reconnecting to {} {}", TYPE_REDIS, candidate.getNewAddress());
        }
        ctx.setAttribute(ATTR_OLD_ADDRESS, candidate);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void onSuccess(ExecutableContext ctx) {
        DbCandidate candidate = ctx.getAttribute(ATTR_OLD_ADDRESS);
        if (candidate == null) {
            return;
        }
        RedisClient client = (RedisClient) ctx.getTarget();
        RedisCodec<?, ?> codec = ctx.getArgument(0);
        RedisURI uri = ctx.getArgument(1);
        Duration timeout = ctx.getArgument(2);
        MethodContext mc = (MethodContext) ctx;
        ConnectionFuture<StatefulRedisConnection<?, ?>> future = mc.getResult();
        Function<RedisURI, CompletionStage<?>> recreator = u -> connect(client, u, codec, timeout);
        mc.setResult(future.thenApply(connection -> checkFailover(
                createConnection(() -> new LettuceStatefulRedisConnection(
                        connection, uri, toClusterRedirect(candidate), closer, recreator)),
                addressResolver)));
    }

    /**
     * Asynchronously establishes a Redis connection with enhanced command handling capabilities.
     *
     * @param client   Redis client instance providing configuration and resources
     * @param redisURI Target server connection parameters
     * @param codec    Data serialization/deserialization codec
     * @param timeout  Connection establishment timeout duration
     * @return CompletionStage tracking connection progress, completes with active connection or failure
     */
    private CompletionStage<?> connect(Object client, Object redisURI, Object codec, Object timeout) {
        return LettuceContext.callAsync(() -> (CompletionStage<?>) Accessor.connect(client, codec, redisURI, timeout));
    }

    private static class Accessor {

        private static final Method connect = ClassUtils.getDeclaredMethod(RedisClient.class,
                "connectStandaloneAsync", new Class[]{RedisCodec.class, RedisURI.class, Duration.class});

        public static Object connect(Object target, Object... args) throws InvocationTargetException, IllegalAccessException {
            return connect.invoke(target, args);
        }

    }
}
