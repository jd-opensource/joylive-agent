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
package com.jd.live.agent.plugin.protection.mysql.v5.definition;

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
import com.jd.live.agent.plugin.protection.mysql.v5.condition.ConditionalOnMysql5ProtectEnabled;
import com.jd.live.agent.plugin.protection.mysql.v5.interceptor.ConnectionImplInterceptor;

@Injectable
@Extension(value = "ConnectionImplDefinition_v5", order = PluginDefinition.ORDER_PROTECT)
@ConditionalOnMysql5ProtectEnabled
@ConditionalOnClass(ConnectionImplDefinition.TYPE)
public class ConnectionImplDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "com.mysql.jdbc.ConnectionImpl";

    private static final String METHOD = "execSQL";

    private static final String[] ARGUMENTS = {
            "com.mysql.jdbc.StatementImpl",
            "java.lang.String",
            "int",
            "com.mysql.jdbc.Buffer",
            "int",
            "int",
            "boolean",
            "java.lang.String",
            "com.mysql.jdbc.Field[]",
            "boolean"
    };

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    public ConnectionImplDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).
                                and(MatcherBuilder.arguments(ARGUMENTS)),
                        () -> new ConnectionImplInterceptor(policySupplier)
                )
        };
    }
}
