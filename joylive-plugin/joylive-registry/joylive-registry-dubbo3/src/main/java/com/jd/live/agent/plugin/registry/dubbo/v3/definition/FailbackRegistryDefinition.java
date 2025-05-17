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
package com.jd.live.agent.plugin.registry.dubbo.v3.definition;

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
import com.jd.live.agent.plugin.registry.dubbo.v3.condition.ConditionalOnDubbo3GovernanceEnabled;
import com.jd.live.agent.plugin.registry.dubbo.v3.interceptor.FailbackRegistryInterceptor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * FailbackRegistryDefinition
 */
@Injectable
@Extension(value = "FailbackRegistryDefinition_v3", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnDubbo3GovernanceEnabled
@ConditionalOnClass(FailbackRegistryDefinition.TYPE_FAILBACK_REGISTRY)
public class FailbackRegistryDefinition extends PluginDefinitionAdapter {

    // for interface register
    protected static final String TYPE_FAILBACK_REGISTRY = "org.apache.dubbo.registry.support.FailbackRegistry";

    // compatible with interface registry
    protected static final String TYPE_SERVICE_DISCOVERY_REGISTRY = "org.apache.dubbo.registry.client.ServiceDiscoveryRegistry";

    private static final String METHOD_REGISTER = "doRegister";

    private static final String[] ARGUMENT_REGISTER = new String[]{
            "org.apache.dubbo.common.URL"
    };

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(Registry.COMPONENT_REGISTRY)
    private Registry registry;

    public FailbackRegistryDefinition() {

        Map<String, Set<String>> conditions = new HashMap<>();
        conditions.computeIfAbsent("org.apache.dubbo.registry.consul.ConsulRegistry", s -> new HashSet<>())
                .add("com.ecwid.consul.v1.ConsulClient");
        conditions.computeIfAbsent("org.apache.dubbo.registry.nacos.NacosRegistry", s -> new HashSet<>())
                .add("com.alibaba.nacos.api.naming.pojo.Instance");
        conditions.computeIfAbsent("org.apache.dubbo.registry.redis.RedisRegistry", s -> new HashSet<>())
                .add("redis.clients.jedis.JedisPubSub");
        this.matcher = () -> MatcherBuilder.isSubTypeOf(TYPE_FAILBACK_REGISTRY)
                .and(MatcherBuilder.not(MatcherBuilder.named(TYPE_SERVICE_DISCOVERY_REGISTRY)))
                .and(MatcherBuilder.exists(conditions));
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_REGISTER)
                                .and(MatcherBuilder.arguments(ARGUMENT_REGISTER))
                                .and(MatcherBuilder.not(MatcherBuilder.isAbstract())),
                        () -> new FailbackRegistryInterceptor(application, registry))
        };
    }
}
