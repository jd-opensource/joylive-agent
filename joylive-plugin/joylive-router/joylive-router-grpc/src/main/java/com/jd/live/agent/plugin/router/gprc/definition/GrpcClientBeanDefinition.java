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
package com.jd.live.agent.plugin.router.gprc.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.ConditionalRelation;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.router.gprc.interceptor.GrpcClientBeanInterceptor;

@Extension(value = "GrpcClientBeanDefinition", order = PluginDefinition.ORDER_ROUTER)
@ConditionalOnProperty(name = {
        GovernanceConfig.CONFIG_LIVE_ENABLED,
        GovernanceConfig.CONFIG_LANE_ENABLED,
        GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED
}, matchIfMissing = true, relation = ConditionalRelation.OR)
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_LIVE_GRPC_ENABLED, matchIfMissing = true)
@ConditionalOnClass(GrpcClientBeanDefinition.TYPE)
public class GrpcClientBeanDefinition extends PluginDefinitionAdapter {

    public static final String TYPE = "net.devh.boot.grpc.client.inject.GrpcClientBeanPostProcessor";

    private static final String METHOD = "processInjectionPoint";

    private static final String[] ARGUMENTS = new String[]{
            "java.lang.reflect.Member",
            "java.lang.Class",
            "net.devh.boot.grpc.client.inject.GrpcClient",
    };

    public GrpcClientBeanDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(ARGUMENTS)),
                        GrpcClientBeanInterceptor::new)
        };

    }

}
