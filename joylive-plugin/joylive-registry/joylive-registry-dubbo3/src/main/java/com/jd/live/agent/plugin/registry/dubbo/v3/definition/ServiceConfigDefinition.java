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
import com.jd.live.agent.plugin.registry.dubbo.v3.interceptor.ServiceConfigInterceptor;

/**
 * ServiceConfigDefinition
 */
@Injectable
@Extension(value = "ServiceConfigDefinition_v3", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnDubbo3GovernanceEnabled
@ConditionalOnClass(ServiceConfigDefinition.TYPE_SERVICE_CONFIG)
public class ServiceConfigDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_SERVICE_CONFIG = "org.apache.dubbo.config.ServiceConfig";

    private static final String METHOD_BUILD_ATTRIBUTES = "buildAttributes";

    private static final String[] ARGUMENT_BUILD_ATTRIBUTES = new String[]{
            "org.apache.dubbo.config.ProtocolConfig"
    };

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(Registry.COMPONENT_REGISTRY)
    private Registry registry;

    public ServiceConfigDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_SERVICE_CONFIG);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_BUILD_ATTRIBUTES).
                                and(MatcherBuilder.arguments(ARGUMENT_BUILD_ATTRIBUTES)),
                        () -> new ServiceConfigInterceptor(application, registry))
        };
    }
}
