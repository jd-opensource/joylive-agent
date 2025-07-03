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
package com.jd.live.agent.plugin.protection.lettuce.v6.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.protection.lettuce.v6.condition.ConditionalOnProtectLettuce6Enabled;
import com.jd.live.agent.plugin.protection.lettuce.v6.interceptor.MultiCommandHandlerInterceptor;
import com.jd.live.agent.plugin.protection.lettuce.v6.interceptor.SingleCommandHandlerInterceptor;

@Injectable
@Extension(value = "RedisChannelHandlerDefinition_v6", order = PluginDefinition.ORDER_PROTECT)
@ConditionalOnProtectLettuce6Enabled
@ConditionalOnClass(RedisChannelHandlerDefinition.TYPE)
public class RedisChannelHandlerDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "io.lettuce.core.RedisChannelHandler";

    private static final String METHOD = "dispatch";

    private static final String[] ARGUMENTS0 = {
            "io.lettuce.core.protocol.RedisCommand"
    };

    private static final String[] ARGUMENTS1 = {
            "java.util.Collection"
    };

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    private GovernanceConfig governanceConfig;

    public RedisChannelHandlerDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(ARGUMENTS0)),
                        () -> new SingleCommandHandlerInterceptor(policySupplier, governanceConfig)
                ),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(ARGUMENTS1)),
                        () -> new MultiCommandHandlerInterceptor(policySupplier, governanceConfig)
                )
        };
    }
}
