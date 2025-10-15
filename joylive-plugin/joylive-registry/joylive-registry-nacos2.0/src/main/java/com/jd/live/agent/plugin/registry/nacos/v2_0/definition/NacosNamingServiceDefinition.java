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
package com.jd.live.agent.plugin.registry.nacos.v2_0.definition;

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
import com.jd.live.agent.plugin.registry.nacos.v2_0.condition.ConditionalOnNacos20GovernanceEnabled;
import com.jd.live.agent.plugin.registry.nacos.v2_0.interceptor.NacosNamingServiceInitInterceptor;
import com.jd.live.agent.plugin.registry.nacos.v2_0.interceptor.NacosNamingServiceShutdownInterceptor;

/**
 * Nacos Naming Service Plugin Definition.
 *
 * <p>This plugin intercepts naming service initialization and shutdown methods
 * to register with and deregister from the Agent's registry center. It ensures
 * proper lifecycle management of the naming service within the Agent framework.</p>
 *
 * <p>The plugin targets Nacos 2.0.x versions and is conditionally loaded when
 * Nacos governance is enabled and the target class is present in the classpath.</p>
 */
@Injectable
@Extension(value = "NacosNamingServiceDefinition_v2.0", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnNacos20GovernanceEnabled
@ConditionalOnClass(NacosNamingServiceDefinition.TYPE)
public class NacosNamingServiceDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "com.alibaba.nacos.client.naming.NacosNamingService";

    private static final String METHOD_SHUTDOWN = "shutdown";

    private static final String METHOD_INIT = "init";

    private static final String[] ARGUMENTS_INIT = new String[]{
            "java.util.Properties"
    };

    @Inject(Registry.COMPONENT_REGISTRY)
    private CompositeRegistry registry;

    public NacosNamingServiceDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_INIT).and(MatcherBuilder.arguments(ARGUMENTS_INIT)),
                        () -> new NacosNamingServiceInitInterceptor(registry)),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_SHUTDOWN), () -> new NacosNamingServiceShutdownInterceptor(registry)),
        };
    }
}
