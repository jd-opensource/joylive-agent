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
package com.jd.live.agent.plugin.protection.jedis.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.protection.jedis.v5.config.JedisConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;

import java.util.Set;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * JedisClusterProtectInterceptor
 */
public class JedisClusterProtectInterceptor extends InterceptorAdaptor {

    @Override
    public void onEnter(ExecutableContext ctx) {
        Set<HostAndPort> clusterNodes = ctx.getArgument(0);
        JedisClientConfig config = ctx.getArgument(1);
        ctx.setArgument(1, new JedisConfig(config, toList(clusterNodes, HostAndPort::toString).toArray(new String[0])));
    }
}
