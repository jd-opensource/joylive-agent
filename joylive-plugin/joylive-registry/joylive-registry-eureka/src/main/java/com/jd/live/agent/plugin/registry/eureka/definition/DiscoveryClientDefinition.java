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
import com.jd.live.agent.governance.registry.CompositeRegistry;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.registry.eureka.interceptor.DiscoveryClientConstructorInterceptor;
import com.jd.live.agent.plugin.registry.eureka.interceptor.DiscoveryClientDeltaUpdateInterceptor;
import com.jd.live.agent.plugin.registry.eureka.interceptor.DiscoveryClientFullUpdateInterceptor;
import com.jd.live.agent.plugin.registry.eureka.interceptor.DiscoveryClientShutdownInterceptor;

/**
 * DiscoveryClientDefinition
 */
@Injectable
@Extension(value = "DiscoveryClientDefinition", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnGovernanceEnabled
@ConditionalOnClass(DiscoveryClientDefinition.TYPE_DISCOVERY_CLIENT)
public class DiscoveryClientDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_DISCOVERY_CLIENT = "com.netflix.discovery.DiscoveryClient";

    private static final String METHOD_GET_AND_STORE_FULL_REGISTRY = "getAndStoreFullRegistry";

    private static final String METHOD_UPDATE_DELTA = "updateDelta";

    private static final String METHOD_SHUTDOWN = "shutdown";

    // for version 2
    private static final String[] ARGUMENTS_CONSTRUCTOR2 = new String[]{
            "com.netflix.appinfo.ApplicationInfoManager",
            "com.netflix.discovery.EurekaClientConfig",
            "com.netflix.discovery.shared.transport.jersey.TransportClientFactories",
            "com.netflix.discovery.AbstractDiscoveryClientOptionalArgs",
            "jakarta.inject.Provider",
            "com.netflix.discovery.shared.resolver.EndpointRandomizer"
    };

    // for version 1
    private static final String[] ARGUMENTS_CONSTRUCTOR1 = new String[]{
            "com.netflix.appinfo.ApplicationInfoManager",
            "com.netflix.discovery.EurekaClientConfig",
            "com.netflix.discovery.AbstractDiscoveryClientOptionalArgs",
            "javax.inject.Provider",
            "com.netflix.discovery.shared.resolver.EndpointRandomizer"
    };

    @Inject(Registry.COMPONENT_REGISTRY)
    private CompositeRegistry registry;

    public DiscoveryClientDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_DISCOVERY_CLIENT);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_GET_AND_STORE_FULL_REGISTRY), () -> new DiscoveryClientFullUpdateInterceptor()),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_UPDATE_DELTA), () -> new DiscoveryClientDeltaUpdateInterceptor()),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.isConstructor().and(MatcherBuilder.arguments(ARGUMENTS_CONSTRUCTOR2)),
                        () -> new DiscoveryClientConstructorInterceptor(registry)),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.isConstructor().and(MatcherBuilder.arguments(ARGUMENTS_CONSTRUCTOR1)),
                        () -> new DiscoveryClientConstructorInterceptor(registry)),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_SHUTDOWN), () -> new DiscoveryClientShutdownInterceptor(registry)),
        };
    }
}
