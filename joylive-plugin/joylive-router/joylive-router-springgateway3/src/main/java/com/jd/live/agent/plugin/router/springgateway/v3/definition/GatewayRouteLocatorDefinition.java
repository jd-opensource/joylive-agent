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
package com.jd.live.agent.plugin.router.springgateway.v3.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.plugin.router.springgateway.v3.condition.ConditionalOnSpringGateway3FlowControlEnabled;
import com.jd.live.agent.plugin.router.springgateway.v3.interceptor.GatewayRouteLocatorInterceptor;

/**
 * GatewayRouteLocatorDefinition
 */
@Extension(value = "GatewayRouteLocatorDefinition_v3")
@ConditionalOnSpringGateway3FlowControlEnabled
@ConditionalOnClass(GatewayRouteLocatorDefinition.TYPE)
@Injectable
public class GatewayRouteLocatorDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator";

    private static final String METHOD = "combinePredicates";

    private static final String[] ARGUMENTS = {
            "org.springframework.cloud.gateway.route.RouteDefinition",
    };

    public GatewayRouteLocatorDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(ARGUMENTS)),
                        () -> new GatewayRouteLocatorInterceptor())
        };
    }
}
