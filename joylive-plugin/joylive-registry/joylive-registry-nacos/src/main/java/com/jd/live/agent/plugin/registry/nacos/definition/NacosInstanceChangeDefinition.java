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
package com.jd.live.agent.plugin.registry.nacos.definition;

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
import com.jd.live.agent.plugin.registry.nacos.interceptor.NacosInstanceChangeInterceptor;

/**
 * NacosInstanceChangeDefinition
 */
@Injectable
@Extension(value = "NacosInstanceChangeDefinition", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnGovernanceEnabled
@ConditionalOnClass(NacosInstanceChangeDefinition.TYPE_INSTANCES_CHANGE_NOTIFIER)
public class NacosInstanceChangeDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_INSTANCES_CHANGE_NOTIFIER = "com.alibaba.nacos.client.naming.event.InstancesChangeNotifier";

    private static final String METHOD_ON_EVENT = "onEvent";

    @Inject(Registry.COMPONENT_REGISTRY)
    private CompositeRegistry registry;

    public NacosInstanceChangeDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_INSTANCES_CHANGE_NOTIFIER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_ON_EVENT),
                        () -> new NacosInstanceChangeInterceptor(registry)),
        };
    }
}
