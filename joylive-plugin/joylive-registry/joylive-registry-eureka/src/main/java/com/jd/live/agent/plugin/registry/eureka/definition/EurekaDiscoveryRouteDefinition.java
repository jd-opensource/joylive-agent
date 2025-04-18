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
package com.jd.live.agent.plugin.registry.eureka.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnGovernanceEnabled;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.registry.eureka.interceptor.EurekaDiscoveryRouteInterceptor;

/**
 * EurekaDiscoveryRouteDefinition
 */
@Injectable
@Extension(value = "EurekaDiscoveryRouteDefinition", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnGovernanceEnabled
@ConditionalOnClass(EurekaDiscoveryRouteDefinition.TYPE_DELEGATING_SERVICE_INSTANCE)
@ConditionalOnClass(EurekaDiscoveryRouteDefinition.TYPE_SERVICE_INSTANCE)
@ConditionalOnClass(EurekaDiscoveryRouteDefinition.TYPE_EUREKA_SERVICE_INSTANCE)
public class EurekaDiscoveryRouteDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_DELEGATING_SERVICE_INSTANCE = "org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator$DelegatingServiceInstance";

    protected static final String TYPE_SERVICE_INSTANCE = "org.springframework.cloud.client.ServiceInstance";

    protected static final String TYPE_EUREKA_SERVICE_INSTANCE = "org.springframework.cloud.netflix.eureka.EurekaServiceInstance";

    @Inject(Registry.COMPONENT_REGISTRY)
    private Registry registry;

    public EurekaDiscoveryRouteDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_DELEGATING_SERVICE_INSTANCE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.isConstructor(), () -> new EurekaDiscoveryRouteInterceptor(registry))
        };
    }
}
