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
package com.jd.live.agent.plugin.registry.nacos.v1_4.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.plugin.registry.nacos.v1_4.condition.ConditionalOnNacos14GovernanceEnabled;
import com.jd.live.agent.plugin.registry.nacos.v1_4.interceptor.NacosNotifyCenterInterceptor;

/**
 * Nacos Notify Center Plugin Definition.
 *
 * <p>This plugin modifies the subscription method to register subscribers with
 * custom notifiers, enabling notification of aggregated instance change events.
 * It intercepts the NotifyCenter's registerSubscriber method to enhance event
 * handling capabilities for service discovery operations.</p>
 *
 * <p>The plugin targets Nacos 1.4.x versions and is conditionally loaded when
 * Nacos governance is enabled and the target class is present in the classpath.</p>
 */
@Injectable
@Extension(value = "NotifyCenterDefinition_v1.4", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnNacos14GovernanceEnabled
@ConditionalOnClass(NacosNotifyCenterDefinition.TYPE)
public class NacosNotifyCenterDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "com.alibaba.nacos.common.notify.NotifyCenter";

    private static final String METHOD = "registerSubscriber";

    private static final String[] ARGUMENTS = new String[]{
            "com.alibaba.nacos.common.notify.listener.Subscriber"
    };

    public NacosNotifyCenterDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(ARGUMENTS)), () -> new NacosNotifyCenterInterceptor())
        };
    }
}
