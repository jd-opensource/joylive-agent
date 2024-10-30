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
import com.jd.live.agent.core.extension.annotation.*;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.interceptor.ServiceConfigInterceptor;

/**
 * ServiceConfigDefinition
 */
@Injectable
@Extension(value = "ServiceConfigDefinition_v2.7", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.OR)
@ConditionalOnClass(ReferenceConfigDefinition.TYPE_PROTOCOL_FILTER_WRAPPER)
@ConditionalOnClass(ServiceConfigDefinition.TYPE_SERVICE_CONFIG)
public class ServiceConfigDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_SERVICE_CONFIG = "org.apache.dubbo.config.ServiceConfig";

    private static final String METHOD_FIND_CONFIGED_HOSTS = "findConfigedHosts";

    private static final String[] ARGUMENT_FIND_CONFIGED_HOSTS = new String[]{
            "org.apache.dubbo.config.ProtocolConfig",
            "java.util.List",
            "java.util.Map"
    };

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    public ServiceConfigDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_SERVICE_CONFIG);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_FIND_CONFIGED_HOSTS)
                                .and(MatcherBuilder.arguments(ARGUMENT_FIND_CONFIGED_HOSTS)),
                        () -> new ServiceConfigInterceptor(application, policySupplier))
        };
    }
}
