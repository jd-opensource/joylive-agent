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

import com.jd.live.agent.core.bootstrap.AgentLifecycle;
import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.*;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.registry.dubbo.v2_6.interceptor.RegistryInterceptor;

/**
 * RegistryDefinition
 */
@Injectable
@Extension(value = "RegistryDefinition_v2.6", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.OR)
@ConditionalOnClass(ReferenceConfigDefinition.TYPE_REFERENCE_CONFIG)
@ConditionalOnClass(RegistryDefinition.TYPE_FAILBACK_REGISTRY)
public class RegistryDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_FAILBACK_REGISTRY = "com.alibaba.dubbo.registry.support.FailbackRegistry";

    private static final String METHOD_REGISTER = "doRegister";

    private static final String[] ARGUMENT_REGISTER = new String[]{
            "com.alibaba.dubbo.common.URL"
    };

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(AgentLifecycle.COMPONENT_AGENT_LIFECYCLE)
    private AgentLifecycle lifecycle;

    @Inject(Registry.COMPONENT_REGISTRY)
    private Registry registry;

    public RegistryDefinition() {
        this.matcher = () -> MatcherBuilder.isSubTypeOf(TYPE_FAILBACK_REGISTRY)
                .and(MatcherBuilder.exists("com.alibaba.dubbo.registry.redis.RedisRegistry",
                        "redis.clients.jedis.Jedis", "org.apache.commons.pool2.impl.GenericObjectPoolConfig"));
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_REGISTER)
                                .and(MatcherBuilder.arguments(ARGUMENT_REGISTER))
                                .and(MatcherBuilder.not(MatcherBuilder.isAbstract())),
                        () -> new RegistryInterceptor(application, lifecycle, registry))
        };
    }
}
