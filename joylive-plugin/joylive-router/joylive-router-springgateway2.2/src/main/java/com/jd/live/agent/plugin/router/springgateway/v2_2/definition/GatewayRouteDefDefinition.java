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
package com.jd.live.agent.plugin.router.springgateway.v2_2.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.plugin.router.springgateway.v2_2.condition.ConditionalOnSpringGateway2GovernanceEnabled;
import com.jd.live.agent.plugin.router.springgateway.v2_2.interceptor.GatewayRouteDefInterceptor;

/**
 * GatewayRouteDefDefinition
 */
@Extension(value = "GatewayRouteDefDefinition_v2.2")
@ConditionalOnSpringGateway2GovernanceEnabled
@ConditionalOnClass(GatewayRouteDefDefinition.TYPE)
@Injectable
public class GatewayRouteDefDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.springframework.cloud.gateway.route.RouteDefinition";

    private static final String METHOD = "setUri";

    public GatewayRouteDefDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD),
                        () -> new GatewayRouteDefInterceptor())
        };
    }
}
