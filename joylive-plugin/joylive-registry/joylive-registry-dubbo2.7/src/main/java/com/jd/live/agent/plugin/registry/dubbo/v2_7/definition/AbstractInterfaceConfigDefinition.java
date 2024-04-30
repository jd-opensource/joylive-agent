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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.interceptor.AppendRuntimeParametersInterceptor;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.interceptor.InitServiceMetadataInterceptor;

/**
 * AbstractInterfaceConfig
 */
@Injectable
@Extension(value = "AbstractInterfaceConfig_v2.7", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_REGISTRY_ENABLED, matchIfMissing = true)
@ConditionalOnClass(AbstractInterfaceConfigDefinition.TYPE_CONSUMER_CONTEXT_FILTER)
@ConditionalOnClass(AbstractInterfaceConfigDefinition.TYPE_ABSTRACT_INTERFACE_CONFIG)
public class AbstractInterfaceConfigDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_ABSTRACT_INTERFACE_CONFIG = "org.apache.dubbo.config.AbstractInterfaceConfig";

    public static final String TYPE_CONSUMER_CONTEXT_FILTER = "org.apache.dubbo.rpc.filter.ConsumerContextFilter";

    private static final String METHOD_APPEND_RUNTIME_PARAMETERS = "appendRuntimeParameters";

    private static final String METHOD_INIT_SERVICE_METADATA = "initServiceMetadata";

    private static final String[] ARGUMENT_APPEND_RUNTIME_PARAMETERS = new String[]{
            "java.util.Map"
    };

    private static final String[] ARGUMENT_INIT_SERVICE_METADATA = new String[]{
            "org.apache.dubbo.config.AbstractInterfaceConfig"
    };

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    public AbstractInterfaceConfigDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_ABSTRACT_INTERFACE_CONFIG);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_APPEND_RUNTIME_PARAMETERS).
                                and(MatcherBuilder.arguments(ARGUMENT_APPEND_RUNTIME_PARAMETERS)),
                        () -> new AppendRuntimeParametersInterceptor(application)),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_INIT_SERVICE_METADATA).
                                and(MatcherBuilder.arguments(ARGUMENT_INIT_SERVICE_METADATA)),
                        () -> new InitServiceMetadataInterceptor(policySupplier))
        };
    }
}
