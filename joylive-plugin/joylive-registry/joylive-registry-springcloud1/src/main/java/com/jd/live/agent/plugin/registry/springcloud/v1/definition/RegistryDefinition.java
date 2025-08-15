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
package com.jd.live.agent.plugin.registry.springcloud.v1.definition;

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
import com.jd.live.agent.plugin.registry.springcloud.v1.condition.ConditionalOnSpringCloud1GovernanceEnabled;
import com.jd.live.agent.plugin.registry.springcloud.v1.interceptor.RegistryInterceptor;

/**
 * RegistryDefinition
 */
@Injectable
@Extension(value = "ServiceRegistryDefinition_v1", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnSpringCloud1GovernanceEnabled
@ConditionalOnClass(RegistryDefinition.TYPE)
public class RegistryDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.springframework.cloud.client.serviceregistry.ServiceRegistry";

    private static final String METHOD = "register";

    private static final String ARGUMENT = "org.springframework.cloud.client.serviceregistry.Registration";

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(Registry.COMPONENT_REGISTRY)
    private Registry registry;

    public RegistryDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).
                                and(MatcherBuilder.arguments(MatcherBuilder.isSubTypeOf(ARGUMENT))),
                        () -> new RegistryInterceptor(application, registry))
        };
    }
}
