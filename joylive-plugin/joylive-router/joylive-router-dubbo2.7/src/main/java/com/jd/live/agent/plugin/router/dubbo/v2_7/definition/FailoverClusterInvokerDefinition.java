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
package com.jd.live.agent.plugin.router.dubbo.v2_7.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.router.dubbo.v2_7.interceptor.FailoverClusterInvokerInterceptor;

@Extension(value = "FailoverClusterInvokerDefinition_v2.7")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_DUBBO_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_REGISTRY_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_TRANSMISSION_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_RETRY_ENABLED, matchIfMissing = true)
@ConditionalOnClass(FailoverClusterInvokerDefinition.TYPE_FAILOVER_CLUSTER_INVOKER)
@ConditionalOnClass(ClassLoaderFilterDefinition.TYPE_PROTOCOL_FILTER_WRAPPER)
public class FailoverClusterInvokerDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_FAILOVER_CLUSTER_INVOKER = "org.apache.dubbo.rpc.cluster.support.FailoverClusterInvoker";

    private static final String METHOD_CALCULATE_INVOKE_TIMES = "calculateInvokeTimes";

    protected static final String[] ARGUMENT_CALCULATE_INVOKE_TIMES = new String[]{
            "java.lang.String"
    };

    public FailoverClusterInvokerDefinition() {
        super(TYPE_FAILOVER_CLUSTER_INVOKER,
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_CALCULATE_INVOKE_TIMES).
                                and(MatcherBuilder.arguments(ARGUMENT_CALCULATE_INVOKE_TIMES)),
                        new FailoverClusterInvokerInterceptor()));
    }
}
