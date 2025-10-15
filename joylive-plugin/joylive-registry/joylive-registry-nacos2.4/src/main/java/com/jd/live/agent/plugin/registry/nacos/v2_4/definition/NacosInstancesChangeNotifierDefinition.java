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
package com.jd.live.agent.plugin.registry.nacos.v2_4.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.plugin.registry.nacos.v2_4.condition.ConditionalOnNacos24GovernanceEnabled;
import com.jd.live.agent.plugin.registry.nacos.v2_4.interceptor.NacosInstancesChangeNotifierInterceptor;

/**
 * Nacos Instances Change Notifier Plugin Definition.
 *
 * <p>This plugin intercepts the instances change notifier to inject
 * custom notifier functionality for capturing instance change events.
 * It enhances the default notifier with additional event handling capabilities.</p>
 *
 * <p>The plugin targets Nacos 2.4.x versions and is conditionally loaded when
 * Nacos governance is enabled and the target class is present in the classpath.</p>
 */
@Injectable
@Extension(value = "NacosInstanceChangeDefinition_v2.4", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnNacos24GovernanceEnabled
@ConditionalOnClass(NacosInstancesChangeNotifierDefinition.TYPE_INSTANCES_CHANGE_NOTIFIER)
public class NacosInstancesChangeNotifierDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_INSTANCES_CHANGE_NOTIFIER = "com.alibaba.nacos.client.naming.event.InstancesChangeNotifier";

    public NacosInstancesChangeNotifierDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_INSTANCES_CHANGE_NOTIFIER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.isConstructor(), () -> new NacosInstancesChangeNotifierInterceptor())
        };
    }
}
