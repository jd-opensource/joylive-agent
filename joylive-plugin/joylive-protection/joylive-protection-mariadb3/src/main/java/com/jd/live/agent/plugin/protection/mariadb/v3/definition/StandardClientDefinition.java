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
package com.jd.live.agent.plugin.protection.mariadb.v3.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.protection.mariadb.v3.condition.ConditionalOnMariadbProtectEnabled;
import com.jd.live.agent.plugin.protection.mariadb.v3.interceptor.SendQueryInterceptor;

@Injectable
@Extension(value = "StandardClientDefinition_v3", order = PluginDefinition.ORDER_PROTECT)
@ConditionalOnMariadbProtectEnabled
@ConditionalOnClass(StandardClientDefinition.TYPE_STANDARD_CLIENT)
public class StandardClientDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_STANDARD_CLIENT = "org.mariadb.jdbc.client.impl.StandardClient";

    private static final String METHOD_SEND_QUERY = "sendQuery";

    private static final String[] ARGUMENT_SEND_QUERY = new String[]{
            "org.mariadb.jdbc.message.ClientMessage"
    };

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    public StandardClientDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_STANDARD_CLIENT);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_SEND_QUERY).
                                and(MatcherBuilder.arguments(ARGUMENT_SEND_QUERY)),
                        () -> new SendQueryInterceptor(policySupplier)
                )
        };
    }
}
