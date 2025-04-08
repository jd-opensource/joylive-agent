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
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnGovernanceEnabled;
import com.jd.live.agent.plugin.registry.nacos.interceptor.NacosServiceDiscoveryInterceptor;

/**
 * NacosServiceDiscoveryDefinition
 */
@Extension(value = "NacosServiceDiscoveryDefinition", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnGovernanceEnabled
@ConditionalOnClass(NacosServiceDiscoveryDefinition.TYPE_NACOS_SERVICE_DISCOVERY)
public class NacosServiceDiscoveryDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_NACOS_SERVICE_DISCOVERY = "com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery";

    private static final String METHOD_HOST_TO_SERVICE_INSTANCE = "hostToServiceInstance";

    public NacosServiceDiscoveryDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_NACOS_SERVICE_DISCOVERY);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_HOST_TO_SERVICE_INSTANCE),
                        () -> new NacosServiceDiscoveryInterceptor()),
        };
    }
}
