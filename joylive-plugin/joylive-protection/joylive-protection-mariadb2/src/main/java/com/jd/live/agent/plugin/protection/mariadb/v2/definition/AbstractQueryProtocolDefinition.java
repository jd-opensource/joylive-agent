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
package com.jd.live.agent.plugin.protection.mariadb.v2.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.protection.mariadb.v2.interceptor.ExecuteBatchStmtInterceptor;
import com.jd.live.agent.plugin.protection.mariadb.v2.interceptor.ExecutePrepareInterceptor;
import com.jd.live.agent.plugin.protection.mariadb.v2.interceptor.ExecuteQueryInterceptor;
import com.jd.live.agent.plugin.protection.mariadb.v2.interceptor.ExecuteServerInterceptor;

@Injectable
@Extension(value = "StandardClientDefinition_v2", order = PluginDefinition.ORDER_PROTECT)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_PROTECT_ENABLED)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_PROTECT_MARIADB_ENABLED, matchIfMissing = true)
@ConditionalOnClass(AbstractQueryProtocolDefinition.TYPE_ABSTRACT_QUERY_PROTOCOL)
public class AbstractQueryProtocolDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_ABSTRACT_QUERY_PROTOCOL = "org.mariadb.jdbc.internal.protocol.AbstractQueryProtocol";

    private static final String METHOD_EXECUTE_QUERY = "executeQuery";

    private static final String[] ARGUMENT_EXECUTE_QUERY_3 = new String[]{
            "boolean",
            "org.mariadb.jdbc.internal.com.read.dao.Results",
            "java.lang.String"
    };

    private static final String[] ARGUMENT_EXECUTE_QUERY_4 = new String[]{
            "boolean",
            "org.mariadb.jdbc.internal.com.read.dao.Results",
            "java.lang.String",
            "java.nio.charset.Charset"
    };

    private static final String[] ARGUMENT_EXECUTE_QUERY_4_1 = new String[]{
            "boolean",
            "org.mariadb.jdbc.internal.com.read.dao.Results",
            "org.mariadb.jdbc.internal.util.dao.ClientPrepareResult",
            "org.mariadb.jdbc.internal.com.send.parameters.ParameterHolder[]"
    };

    private static final String[] ARGUMENT_EXECUTE_QUERY_5 = new String[]{
            "boolean",
            "org.mariadb.jdbc.internal.com.read.dao.Results",
            "org.mariadb.jdbc.internal.util.dao.ClientPrepareResult",
            "org.mariadb.jdbc.internal.com.send.parameters.ParameterHolder[]",
            "int"
    };


    private static final String METHOD_EXECUTE_BATCH_CLIENT = "executeBatchClient";

    private static final String[] ARGUMENT_EXECUTE_BATCH_CLIENT = new String[]{
            "boolean",
            "org.mariadb.jdbc.internal.com.read.dao.Results",
            "org.mariadb.jdbc.internal.util.dao.ClientPrepareResult",
            "java.util.List",
            "boolean"
    };

    private static final String METHOD_EXECUTE_BATCH_SERVER = "executeBatchServer";

    private static final String[] ARGUMENT_EXECUTE_BATCH_SERVER = new String[]{
            "boolean",
            "org.mariadb.jdbc.internal.util.dao.ServerPrepareResult",
            "org.mariadb.jdbc.internal.com.read.dao.Results",
            "java.lang.String",
            "java.util.List",
            "boolean"
    };

    private static final String METHOD_EXECUTE_BATCH_STMT = "executeBatchStmt";

    private static final String[] ARGUMENT_EXECUTE_BATCH_STMT = new String[]{
            "boolean",
            "org.mariadb.jdbc.internal.com.read.dao.Results",
            "java.util.List"
    };

    private static final String METHOD_EXECUTE_PREPARED_QUERY = "executePreparedQuery";

    private static final String[] ARGUMENT_EXECUTE_PREPARED_QUERY = new String[]{
            "boolean",
            "org.mariadb.jdbc.internal.util.dao.ServerPrepareResult",
            "org.mariadb.jdbc.internal.com.read.dao.Results",
            "org.mariadb.jdbc.internal.com.send.parameters.ParameterHolder[]"
    };

    private static final String METHOD_PREPARE = "prepare";

    private static final String[] ARGUMENT_PREPARE = new String[]{
            "java.lang.String",
            "boolean"
    };


    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    public AbstractQueryProtocolDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_ABSTRACT_QUERY_PROTOCOL);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_EXECUTE_QUERY).
                                and(MatcherBuilder.arguments(ARGUMENT_EXECUTE_QUERY_3).
                                        or(MatcherBuilder.arguments(ARGUMENT_EXECUTE_QUERY_4)).
                                        or(MatcherBuilder.arguments(ARGUMENT_EXECUTE_QUERY_4_1)).
                                        or(MatcherBuilder.arguments(ARGUMENT_EXECUTE_QUERY_5))),
                        () -> new ExecuteQueryInterceptor(policySupplier)
                ),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_EXECUTE_BATCH_CLIENT).
                                and(MatcherBuilder.arguments(ARGUMENT_EXECUTE_BATCH_CLIENT)),
                        () -> new ExecuteQueryInterceptor(policySupplier)
                ),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_EXECUTE_BATCH_STMT).
                                and(MatcherBuilder.arguments(ARGUMENT_EXECUTE_BATCH_STMT)),
                        () -> new ExecuteBatchStmtInterceptor(policySupplier)
                ),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_EXECUTE_BATCH_SERVER).
                                and(MatcherBuilder.arguments(ARGUMENT_EXECUTE_BATCH_SERVER)),
                        () -> new ExecuteServerInterceptor(policySupplier)
                ),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_EXECUTE_PREPARED_QUERY).
                                and(MatcherBuilder.arguments(ARGUMENT_EXECUTE_PREPARED_QUERY)),
                        () -> new ExecuteServerInterceptor(policySupplier)
                ),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_PREPARE).
                                and(MatcherBuilder.arguments(ARGUMENT_PREPARE)),
                        () -> new ExecutePrepareInterceptor(policySupplier)
                )
        };
    }
}
