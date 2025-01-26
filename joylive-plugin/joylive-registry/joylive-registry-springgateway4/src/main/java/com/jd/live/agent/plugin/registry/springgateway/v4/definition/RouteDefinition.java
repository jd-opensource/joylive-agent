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
package com.jd.live.agent.plugin.registry.springgateway.v4.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.registry.springgateway.v4.condition.ConditionalOnSpringGateway4GovernanceEnabled;
import com.jd.live.agent.plugin.registry.springgateway.v4.interceptor.RouteInterceptor;

/**
 * RouteDefinition
 */
@Injectable
@Extension(value = "RouteDefinition_v4", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnSpringGateway4GovernanceEnabled
@ConditionalOnClass(RouteDefinition.TYPE_ROUTE_DEFINITION_ROUTE_LOCATOR)
public class RouteDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_ROUTE_DEFINITION_ROUTE_LOCATOR = "org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator";

    private static final String METHOD_CONVERT_TO_ROUTE = "convertToRoute";

    private static final String[] ARGUMENT_CONVERT_TO_ROUTE = new String[]{
            "org.springframework.cloud.gateway.route.RouteDefinition"
    };

    @Inject(Registry.COMPONENT_REGISTRY)
    private Registry registry;

    public RouteDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_ROUTE_DEFINITION_ROUTE_LOCATOR);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_CONVERT_TO_ROUTE).
                                and(MatcherBuilder.arguments(ARGUMENT_CONVERT_TO_ROUTE)),
                        () -> new RouteInterceptor(registry))
        };
    }
}
