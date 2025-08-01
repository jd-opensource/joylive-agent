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
package com.jd.live.agent.plugin.protection.jedis.v5.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.plugin.protection.jedis.v5.condition.ConditionalOnProtectJedis5Enabled;
import com.jd.live.agent.plugin.protection.jedis.v5.interceptor.JedisSentinelProtectInterceptor;

@Injectable
@Extension(value = "JedisSentinelProtectDefinition_v4", order = PluginDefinition.ORDER_FAILOVER)
@ConditionalOnProtectJedis5Enabled
@ConditionalOnClass(JedisSentinelProtectDefinition.TYPE)
public class JedisSentinelProtectDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "redis.clients.jedis.JedisSentinelPool";

    private static final String[] ARGUMENTS0 = {
            "java.lang.String",
            "java.util.Set",
            "redis.clients.jedis.JedisFactory",
            "redis.clients.jedis.JedisClientConfig"
    };

    private static final String[] ARGUMENTS1 = {
            "java.lang.String",
            "java.util.Set",
            "org.apache.commons.pool2.impl.GenericObjectPoolConfig",
            "redis.clients.jedis.JedisFactory",
            "redis.clients.jedis.JedisClientConfig"
    };

    public JedisSentinelProtectDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.isConstructor().and(MatcherBuilder.arguments(ARGUMENTS0)),
                        () -> new JedisSentinelProtectInterceptor()),
                new InterceptorDefinitionAdapter(MatcherBuilder.isConstructor().and(MatcherBuilder.arguments(ARGUMENTS1)),
                        () -> new JedisSentinelProtectInterceptor()),
        };
    }
}
