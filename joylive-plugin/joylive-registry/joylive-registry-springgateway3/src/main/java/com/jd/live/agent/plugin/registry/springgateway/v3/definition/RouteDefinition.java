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
package com.jd.live.agent.plugin.registry.springgateway.v3.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.*;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.registry.springgateway.v3.interceptor.RouteInterceptor;

/**
 * RouteDefinition
 */
@Injectable
@Extension(value = "RouteDefinition_v3", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.OR)
@ConditionalOnClass(RouteDefinition.TYPE_ROUTE_DEFINITION_ROUTE_LOCATOR)
@ConditionalOnMissingClass(RouteDefinition.TYPE_HTTP_STATUS_CODE)
public class RouteDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_ROUTE_DEFINITION_ROUTE_LOCATOR = "org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator";

    protected static final String TYPE_HTTP_STATUS_CODE = "org.springframework.http.HttpStatusCode";

    private static final String METHOD_CONVERT_TO_ROUTE = "convertToRoute";

    private static final String[] ARGUMENT_CONVERT_TO_ROUTE = new String[]{
            "org.springframework.cloud.gateway.route.RouteDefinition"
    };

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    public RouteDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE_ROUTE_DEFINITION_ROUTE_LOCATOR);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_CONVERT_TO_ROUTE).
                                and(MatcherBuilder.arguments(ARGUMENT_CONVERT_TO_ROUTE)),
                        () -> new RouteInterceptor(policySupplier))
        };
    }
}
