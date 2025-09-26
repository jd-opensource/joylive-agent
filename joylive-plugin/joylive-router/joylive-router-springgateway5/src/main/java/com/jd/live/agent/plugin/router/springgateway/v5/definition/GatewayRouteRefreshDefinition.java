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
package com.jd.live.agent.plugin.router.springgateway.v5.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.plugin.router.springgateway.v5.condition.ConditionalOnSpringGateway5FlowControlEnabled;
import com.jd.live.agent.plugin.router.springgateway.v5.interceptor.GatewayRouteRefreshInterceptor;

/**
 * GatewayRouteRefreshDefinition
 */
@Extension(value = "GatewayRouteDefinition_v5")
@ConditionalOnSpringGateway5FlowControlEnabled
@ConditionalOnClass(GatewayRouteRefreshDefinition.TYPE_REFRESH_ROUTES_EVENT)
@Injectable
public class GatewayRouteRefreshDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_REFRESH_ROUTES_EVENT = "org.springframework.cloud.gateway.event.RefreshRoutesEvent";

    public GatewayRouteRefreshDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_REFRESH_ROUTES_EVENT);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.isConstructor(),
                        () -> new GatewayRouteRefreshInterceptor())
        };
    }
}
