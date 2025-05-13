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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.definition;

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
import com.jd.live.agent.plugin.registry.dubbo.v2_7.condition.ConditionalOnDubbo27GovernanceEnabled;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.interceptor.FailbackRegistryInterceptor;

/**
 * FailbackRegistryDefinition
 */
@Injectable
@Extension(value = "FailbackRegistryDefinition_v2.7", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnDubbo27GovernanceEnabled
@ConditionalOnClass(FailbackRegistryDefinition.TYPE_FAILBACK_REGISTRY)
public class FailbackRegistryDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_FAILBACK_REGISTRY = "org.apache.dubbo.registry.support.FailbackRegistry";

    private static final String METHOD_REGISTER = "doRegister";

    private static final String[] ARGUMENT_REGISTER = new String[]{
            "org.apache.dubbo.common.URL"
    };

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(Registry.COMPONENT_REGISTRY)
    private Registry registry;

    public FailbackRegistryDefinition() {
        this.matcher = () -> MatcherBuilder.isSubTypeOf(TYPE_FAILBACK_REGISTRY);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_REGISTER)
                                .and(MatcherBuilder.arguments(ARGUMENT_REGISTER))
                                .and(MatcherBuilder.not(MatcherBuilder.isAbstract())),
                        () -> new FailbackRegistryInterceptor(application, registry))
        };
    }
}
