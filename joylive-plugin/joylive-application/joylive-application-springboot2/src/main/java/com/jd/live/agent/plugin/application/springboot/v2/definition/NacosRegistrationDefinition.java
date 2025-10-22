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
package com.jd.live.agent.plugin.application.springboot.v2.definition;

import com.jd.live.agent.core.bootstrap.AppListener;
import com.jd.live.agent.core.bootstrap.AppListenerSupervisor;
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
import com.jd.live.agent.plugin.application.springboot.v2.interceptor.NacosRegistrationInterceptor;

/**
 * NacosRegistrationDefinition
 */
@Injectable
@Extension(value = "NacosRegistrationDefinition", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnGovernanceEnabled
@ConditionalOnClass(NacosRegistrationDefinition.TYPE_NACOS_AUTO_SERVICE_REGISTRATION)
public class NacosRegistrationDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_NACOS_AUTO_SERVICE_REGISTRATION = "com.alibaba.cloud.nacos.registry.NacosAutoServiceRegistration";

    @Inject(value = AppListener.COMPONENT_APPLICATION_LISTENER, component = true)
    private AppListenerSupervisor supervisor;

    public NacosRegistrationDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_NACOS_AUTO_SERVICE_REGISTRATION);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.isConstructor(),
                        () -> new NacosRegistrationInterceptor(supervisor)),
        };
    }
}
