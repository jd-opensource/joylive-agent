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
package com.jd.live.agent.plugin.registry.springcloud.v2_2.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnReactive;
import com.jd.live.agent.governance.registry.CompositeRegistry;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.registry.springcloud.v2_2.condition.ConditionalOnSpringCloud2RegistryEnabled;
import com.jd.live.agent.plugin.registry.springcloud.v2_2.interceptor.SimpleDiscoveryClientInterceptor;

/**
 * SimpleReactiveDiscoveryClientDefinition
 */
@Extension(value = "SimpleReactiveDiscoveryClientDefinition_v2.2")
@ConditionalOnSpringCloud2RegistryEnabled
@ConditionalOnReactive
@ConditionalOnClass(SimpleReactiveDiscoveryClientDefinition.TYPE_SIMPLE_REACTIVE_DISCOVERY_CLIENT)
@Injectable
public class SimpleReactiveDiscoveryClientDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_SIMPLE_REACTIVE_DISCOVERY_CLIENT = "org.springframework.cloud.client.discovery.simple.SimpleReactiveDiscoveryClient";

    @Inject(Registry.COMPONENT_REGISTRY)
    private CompositeRegistry registry;

    public SimpleReactiveDiscoveryClientDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE_SIMPLE_REACTIVE_DISCOVERY_CLIENT);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.isConstructor(),
                        () -> new SimpleDiscoveryClientInterceptor(registry)
                )
        };
    }
}
