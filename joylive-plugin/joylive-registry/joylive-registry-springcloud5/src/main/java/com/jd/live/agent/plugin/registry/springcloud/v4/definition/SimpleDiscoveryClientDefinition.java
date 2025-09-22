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
package com.jd.live.agent.plugin.registry.springcloud.v5.definition;

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
import com.jd.live.agent.plugin.registry.springcloud.v5.condition.ConditionalOnSpringCloud4RegistryEnabled;
import com.jd.live.agent.plugin.registry.springcloud.v5.interceptor.SimpleDiscoveryClientInterceptor;

/**
 * SimpleDiscoveryClientDefinition
 */
@Extension(value = "SimpleDiscoveryClientDefinition_v5")
@ConditionalOnSpringCloud4RegistryEnabled
@ConditionalOnReactive
@ConditionalOnClass(SimpleDiscoveryClientDefinition.TYPE_SIMPLE_DISCOVERY_CLIENT)
@Injectable
public class SimpleDiscoveryClientDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_SIMPLE_DISCOVERY_CLIENT = "org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClient";

    @Inject(Registry.COMPONENT_REGISTRY)
    private CompositeRegistry registry;

    public SimpleDiscoveryClientDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE_SIMPLE_DISCOVERY_CLIENT);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.isConstructor(),
                        () -> new SimpleDiscoveryClientInterceptor(registry)
                )
        };
    }
}
