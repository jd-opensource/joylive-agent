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
package com.jd.live.agent.plugin.protection.mongodb.v4.definition;

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
import com.jd.live.agent.plugin.protection.mongodb.v4.condition.ConditionalOnProtectMongodbEnabled;
import com.jd.live.agent.plugin.protection.mongodb.v4.interceptor.ExecuteCommandInterceptor;
import com.jd.live.agent.plugin.protection.mongodb.v4.interceptor.ExecuteRetryableCommandInterceptor;
import com.jd.live.agent.plugin.protection.mongodb.v4.interceptor.ExecuteWriteCommandInterceptor;

@Injectable
@Extension(value = "CommandOperationHelperDefinition_v4", order = PluginDefinition.ORDER_PROTECT)
@ConditionalOnProtectMongodbEnabled
@ConditionalOnClass(CommandOperationHelperDefinition.TYPE_COMMAND_OPERATION_HELPER)
public class CommandOperationHelperDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_COMMAND_OPERATION_HELPER = "com.mongodb.internal.operation.CommandOperationHelper";

    private static final String METHOD_EXECUTE_COMMAND = "executeCommand";

    private static final String METHOD_EXECUTE_WRITE_COMMAND = "executeWriteCommand";

    private static final String METHOD_EXECUTE_RETRYABLE_COMMAND = "executeRetryableCommand";

    private static final String[] ARGUMENT_EXECUTE_COMMAND = {
            "java.lang.String",
            "org.bson.BsonDocument",
            "org.bson.FieldNameValidator",
            "org.bson.codecs.Decoder",
            "com.mongodb.internal.binding.ConnectionSource",
            "com.mongodb.internal.connection.Connection",
            "com.mongodb.ReadPreference"
    };

    private static final String[] ARGUMENT_EXECUTE_WRITE_COMMAND = {
            "java.lang.String",
            "org.bson.BsonDocument",
            "org.bson.FieldNameValidator",
            "org.bson.codecs.Decoder",
            "com.mongodb.internal.connection.Connection",
            "com.mongodb.ReadPreference",
            "com.mongodb.internal.operation.CommandOperationHelper.CommandWriteTransformer",
            "com.mongodb.internal.session.SessionContext",
            "com.mongodb.ServerApi"
    };

    private static final String[] ARGUMENT_EXECUTE_RETRYABLE_COMMAND = {
            "com.mongodb.internal.binding.WriteBinding",
            "java.lang.String",
            "com.mongodb.ReadPreference",
            "org.bson.FieldNameValidator",
            "org.bson.codecs.Decoder",
            "com.mongodb.internal.operation.CommandOperationHelper.CommandCreator",
            "com.mongodb.internal.operation.CommandOperationHelper.CommandWriteTransformer",
            "com.mongodb.Function"
    };

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    public CommandOperationHelperDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_COMMAND_OPERATION_HELPER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_EXECUTE_COMMAND).
                                and(MatcherBuilder.arguments(ARGUMENT_EXECUTE_COMMAND)),
                        () -> new ExecuteCommandInterceptor(policySupplier)
                ),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_EXECUTE_WRITE_COMMAND).
                                and(MatcherBuilder.arguments(ARGUMENT_EXECUTE_WRITE_COMMAND)),
                        () -> new ExecuteWriteCommandInterceptor(policySupplier)
                ),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_EXECUTE_RETRYABLE_COMMAND).
                                and(MatcherBuilder.arguments(ARGUMENT_EXECUTE_RETRYABLE_COMMAND)),
                        () -> new ExecuteRetryableCommandInterceptor(policySupplier)
                )
        };
    }
}
