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
package com.jd.live.agent.plugin.registry.springcloud.v3.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.*;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.registry.springcloud.v3.interceptor.DiscoveryClientConstructorInterceptor;
import com.jd.live.agent.plugin.registry.springcloud.v3.interceptor.DiscoveryClientGetInterceptor;

/**
 * ServiceRegistryDefinition
 */
@Injectable
@Extension(value = "DiscoveryClientDefinition_v3", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.OR)
@ConditionalOnClass(DiscoveryClientDefinition.TYPE_DISCOVERY_CLIENT)
public class DiscoveryClientDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_DISCOVERY_CLIENT = "org.springframework.cloud.loadbalancer.core.DiscoveryClientServiceInstanceListSupplier";

    private static final String METHOD_GET = "get";

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    public DiscoveryClientDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE_DISCOVERY_CLIENT);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.isConstructor(), () -> new DiscoveryClientConstructorInterceptor(policySupplier)),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_GET)
                                .and(MatcherBuilder.arguments(0)),
                        () -> new DiscoveryClientGetInterceptor(policySupplier))
        };
    }
}
