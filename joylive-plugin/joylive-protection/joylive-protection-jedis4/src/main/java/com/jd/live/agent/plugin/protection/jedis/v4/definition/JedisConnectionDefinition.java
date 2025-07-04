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
package com.jd.live.agent.plugin.protection.jedis.v4.definition;

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
import com.jd.live.agent.plugin.protection.jedis.v4.condition.ConditionalOnProtectJedis4Enabled;
import com.jd.live.agent.plugin.protection.jedis.v4.interceptor.JedisConnectionInterceptor;

@Injectable
@Extension(value = "JedisConnectionDefinition_v4", order = PluginDefinition.ORDER_PROTECT)
@ConditionalOnProtectJedis4Enabled
@ConditionalOnClass(JedisConnectionDefinition.TYPE_JEDIS)
public class JedisConnectionDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_JEDIS = "redis.clients.jedis.Connection";

    private static final String METHOD_SEND_COMMAND = "sendCommand";

    private static final String[] ARGUMENT_SEND_COMMAND = {
            "redis.clients.jedis.CommandArguments"
    };

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    private GovernanceConfig governanceConfig;

    public JedisConnectionDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_JEDIS);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_SEND_COMMAND).
                                and(MatcherBuilder.arguments(ARGUMENT_SEND_COMMAND)),
                        () -> new JedisConnectionInterceptor(policySupplier, governanceConfig)
                )
        };
    }
}
