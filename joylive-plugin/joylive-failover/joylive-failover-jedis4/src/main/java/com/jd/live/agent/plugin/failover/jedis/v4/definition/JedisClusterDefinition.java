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
package com.jd.live.agent.plugin.failover.jedis.v4.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.failover.jedis.v4.condition.ConditionalOnFailoverJedis4Enabled;
import com.jd.live.agent.plugin.failover.jedis.v4.interceptor.JedisClusterInterceptor;

@Injectable
@Extension(value = "JedisClusterDefinition_v4", order = PluginDefinition.ORDER_FAILOVER)
@ConditionalOnFailoverJedis4Enabled
@ConditionalOnClass(JedisClusterDefinition.TYPE)
public class JedisClusterDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "redis.clients.jedis.JedisCluster";

    private static final String[] ARGUMENTS1 = {
            "java.util.Set",
            "redis.clients.jedis.JedisClientConfig",
            "int"
    };

    private static final String[] ARGUMENTS2 = {
            "java.util.Set",
            "redis.clients.jedis.JedisClientConfig",
            "int",
            "java.time.Duration"
    };

    private static final String[] ARGUMENTS3 = {
            "java.util.Set",
            "redis.clients.jedis.JedisClientConfig",
            "int",
            "java.time.Duration",
            "org.apache.commons.pool2.impl.GenericObjectPoolConfig",
    };

    private static final String[] ARGUMENTS4 = {
            "redis.clients.jedis.providers.ClusterConnectionProvider",
            "int",
            "java.time.Duration"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public JedisClusterDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.isConstructor().and(MatcherBuilder.arguments(ARGUMENTS1)),
                        () -> new JedisClusterInterceptor(context)),
                new InterceptorDefinitionAdapter(MatcherBuilder.isConstructor().and(MatcherBuilder.arguments(ARGUMENTS2)),
                        () -> new JedisClusterInterceptor(context)),
                new InterceptorDefinitionAdapter(MatcherBuilder.isConstructor().and(MatcherBuilder.arguments(ARGUMENTS3)),
                        () -> new JedisClusterInterceptor(context)),
                new InterceptorDefinitionAdapter(MatcherBuilder.isConstructor().and(MatcherBuilder.arguments(ARGUMENTS4)),
                        () -> new JedisClusterInterceptor(context)),
        };
    }
}
