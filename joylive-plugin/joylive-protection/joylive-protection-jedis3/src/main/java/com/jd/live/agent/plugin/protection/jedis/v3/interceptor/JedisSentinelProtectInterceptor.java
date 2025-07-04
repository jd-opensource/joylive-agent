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
package com.jd.live.agent.plugin.protection.jedis.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.protection.jedis.v3.config.JedisConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisFactory;

import java.util.Set;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * JedisSentinelProtectInterceptor
 */
public class JedisSentinelProtectInterceptor extends InterceptorAdaptor {

    @Override
    public void onEnter(ExecutableContext ctx) {
        Set<HostAndPort> sentinels = ctx.getArgument(1);
        JedisFactory factory = ctx.getArgument(ctx.getArgumentCount() - 2);
        JedisClientConfig config = Accessor.clientConfig.get(factory, JedisClientConfig.class);
        Accessor.clientConfig.set(factory, new JedisConfig(config, toList(sentinels, HostAndPort::toString).toArray(new String[0])));
    }

    private static class Accessor {

        private static final UnsafeFieldAccessor clientConfig = UnsafeFieldAccessorFactory.getAccessor(JedisFactory.class, "clientConfig");

    }
}
