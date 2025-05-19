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
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.registry.dubbo.v2_6.condition.ConditionalOnDubbo26GovernanceEnabled;
import com.jd.live.agent.plugin.registry.dubbo.v2_6.interceptor.FailbackRegistryConstructorInterceptor;
import com.jd.live.agent.plugin.registry.dubbo.v2_6.interceptor.FailbackRegistryRegisterInterceptor;
import com.jd.live.agent.plugin.registry.dubbo.v2_6.interceptor.FailbackRegistrySubscribeInterceptor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * FailbackRegistryDefinition
 */
@Injectable
@Extension(value = "FailbackRegistryDefinition_v2.6", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnDubbo26GovernanceEnabled
@ConditionalOnClass(FailbackRegistryDefinition.TYPE_FAILBACK_REGISTRY)
public class FailbackRegistryDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_FAILBACK_REGISTRY = "com.alibaba.dubbo.registry.support.FailbackRegistry";

    private static final String METHOD_REGISTER = "doRegister";

    private static final String[] ARGUMENT_REGISTER = new String[]{
            "com.alibaba.dubbo.common.URL"
    };

    private static final String METHOD_SUBSCRIBE = "doSubscribe";

    private static final String[] ARGUMENT_SUBSCRIBE = new String[]{
            "com.alibaba.dubbo.common.URL",
            "com.alibaba.dubbo.registry.NotifyListener"
    };

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(Registry.COMPONENT_REGISTRY)
    private Registry registry;

    public FailbackRegistryDefinition() {

        Map<String, Set<String>> conditions = new HashMap<>();
        conditions.computeIfAbsent("com.alibaba.dubbo.registry.redis.RedisRegistry", s -> new HashSet<>())
                .add("redis.clients.jedis.JedisPubSub");

        this.matcher = () -> MatcherBuilder.isSubTypeOf(TYPE_FAILBACK_REGISTRY)
                .and(MatcherBuilder.not(MatcherBuilder.isAbstract()))
                .and(MatcherBuilder.exists(conditions));
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_REGISTER).and(MatcherBuilder.arguments(ARGUMENT_REGISTER)),
                        () -> new FailbackRegistryRegisterInterceptor(application, registry)),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_SUBSCRIBE).and(MatcherBuilder.arguments(ARGUMENT_SUBSCRIBE)),
                        () -> new FailbackRegistrySubscribeInterceptor()),
                new InterceptorDefinitionAdapter(MatcherBuilder.isConstructor(),
                        () -> new FailbackRegistryConstructorInterceptor()),
        };
    }
}
