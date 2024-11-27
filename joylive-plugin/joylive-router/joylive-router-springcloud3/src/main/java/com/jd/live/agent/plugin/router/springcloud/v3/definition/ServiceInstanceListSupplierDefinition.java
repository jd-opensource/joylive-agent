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
package com.jd.live.agent.plugin.router.springcloud.v3.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.*;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springcloud.v3.interceptor.ServiceInstanceListSupplierInterceptor;

import java.util.Set;

/**
 * ServiceInstanceListSupplierPluginDefinition
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Injectable
@Extension(value = "ServiceInstanceListSupplierPluginDefinition_v3")
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(name = {
                GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED,
                GovernanceConfig.CONFIG_LIVE_ENABLED,
                GovernanceConfig.CONFIG_LANE_ENABLED
        }, matchIfMissing = true, relation = ConditionalRelation.OR),
        @ConditionalOnProperty(name = GovernanceConfig.CONFIG_LIVE_SPRING_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.AND)
@ConditionalOnClass(ServiceInstanceListSupplierDefinition.TYPE_SERVICE_INSTANCE_LIST_SUPPLIER)
@ConditionalOnClass(BlockingClusterDefinition.TYPE_STICKY_SESSION_SUPPLIER)
@ConditionalOnMissingClass(BlockingClusterDefinition.TYPE_HTTP_STATUS_CODE)
public class ServiceInstanceListSupplierDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_SERVICE_INSTANCE_LIST_SUPPLIER = "org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier";

    private static final String METHOD_GET = "get";

    private static final String[] ARGUMENTS_GET = new String[]{
            "org.springframework.cloud.client.loadbalancer.Request"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    @Config(GovernanceConfig.CONFIG_ROUTER_SPRING_DISCOVERY_DISABLES)
    private Set<String> disableDiscovery;

    public ServiceInstanceListSupplierDefinition() {
        // enhance default method. so isImplementOf is not used.
        this.matcher = () -> MatcherBuilder.isSubTypeOf(TYPE_SERVICE_INSTANCE_LIST_SUPPLIER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_GET).
                                and(MatcherBuilder.arguments(ARGUMENTS_GET)),
                        () -> new ServiceInstanceListSupplierInterceptor(context, disableDiscovery)
                )
        };
    }
}
