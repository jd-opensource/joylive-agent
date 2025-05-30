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
package com.jd.live.agent.plugin.registry.dubbo.v2_6.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.registry.CompositeRegistry;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.registry.dubbo.v2_6.condition.ConditionalOnDubbo26GovernanceEnabled;
import com.jd.live.agent.plugin.registry.dubbo.v2_6.interceptor.RegistryFactoryInterceptor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * RegistryFactoryDefinition
 */
@Injectable
@Extension(value = "RegistryFactoryDefinition_v2.6", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnDubbo26GovernanceEnabled
@ConditionalOnClass(RegistryFactoryDefinition.TYPE)
public class RegistryFactoryDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "com.alibaba.dubbo.registry.support.AbstractRegistryFactory";

    private static final String METHOD = "createRegistry";

    private static final String[] ARGUMENTS = new String[]{
            "com.alibaba.dubbo.common.URL"
    };

    @Inject(Registry.COMPONENT_REGISTRY)
    private CompositeRegistry registry;

    public RegistryFactoryDefinition() {

        Map<String, Set<String>> conditions = new HashMap<>();
        conditions.computeIfAbsent("com.alibaba.dubbo.registry.redis.RedisRegistryFactory", s -> new HashSet<>())
                .add("redis.clients.jedis.JedisPubSub");

        this.matcher = () -> MatcherBuilder.isSubTypeOf(TYPE)
                .and(MatcherBuilder.not(MatcherBuilder.isAbstract()))
                .and(MatcherBuilder.exists(conditions));
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(ARGUMENTS)),
                        () -> new RegistryFactoryInterceptor(registry)),
        };
    }
}
