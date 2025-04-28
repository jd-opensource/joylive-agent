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
package com.jd.live.agent.plugin.protection.mysql.v6.definition;

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
import com.jd.live.agent.plugin.protection.mysql.v6.condition.ConditionalOnMysql6ProtectEnabled;
import com.jd.live.agent.plugin.protection.mysql.v6.interceptor.CreateNewIOInterceptor;
import com.jd.live.agent.plugin.protection.mysql.v6.interceptor.ExecSqlInterceptor;
import com.jd.live.agent.plugin.protection.mysql.v6.interceptor.IsReadOnlyInterceptor;
import com.jd.live.agent.plugin.protection.mysql.v6.interceptor.ResetServerStateInterceptor;

@Injectable
@Extension(value = "ConnectionImplDefinition_v6", order = PluginDefinition.ORDER_PROTECT)
@ConditionalOnMysql6ProtectEnabled
@ConditionalOnClass(ConnectionImplDefinition.TYPE)
public class ConnectionImplDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "com.mysql.jdbc.ConnectionImpl";

    private static final String METHOD_EXEC_SQL = "execSQL";

    private static final String METHOD_CREATE_NEW_IO = "createNewIO";

    private static final String METHOD_IS_READONLY = "isReadOnly";

    private static final String METHOD_RESET_SERVER_STATE = "resetServerState";

    private static final String[] ARGUMENTS_EXEC_SQL = {
            "com.mysql.jdbc.StatementImpl",
            "java.lang.String",
            "int",
            "com.mysql.cj.api.mysqla.io.PacketPayload",
            "boolean",
            "java.lang.String",
            "com.mysql.cj.api.mysqla.result.ColumnDefinition",
            "boolean"
    };

    private static final String[] ARGUMENTS_IS_READONLY = {
            "boolean"
    };

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    public ConnectionImplDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_CREATE_NEW_IO),
                        () -> new CreateNewIOInterceptor(policySupplier)),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_IS_READONLY).and(MatcherBuilder.arguments(ARGUMENTS_IS_READONLY)),
                        () -> new IsReadOnlyInterceptor(policySupplier)),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_RESET_SERVER_STATE),
                        () -> new ResetServerStateInterceptor(policySupplier)),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_EXEC_SQL).and(MatcherBuilder.arguments(ARGUMENTS_EXEC_SQL)),
                        () -> new ExecSqlInterceptor(policySupplier))
        };
    }
}
