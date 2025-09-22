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
package com.jd.live.agent.plugin.registry.springgateway.v5.definition;

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
import com.jd.live.agent.plugin.registry.springgateway.v5.condition.ConditionalOnSpringGateway4GovernanceEnabled;
import com.jd.live.agent.plugin.registry.springgateway.v5.interceptor.RouteInterceptor;

/**
 * RouteDefinition
 */
@Injectable
@Extension(value = "RouteDefinition_v5", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnSpringGateway4GovernanceEnabled
@ConditionalOnClass(RouteDefinition.TYPE)
public class RouteDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.springframework.cloud.gateway.route.Route";

    private static final String[] ARGUMENTS = new String[]{
            "java.lang.String",
            "java.net.URI",
            "int",
            "org.springframework.cloud.gateway.handler.AsyncPredicate",
            "java.util.List",
            "java.util.Map"
    };

    @Inject(Registry.COMPONENT_REGISTRY)
    private Registry registry;

    public RouteDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.isConstructor().and(MatcherBuilder.arguments(ARGUMENTS)),
                        () -> new RouteInterceptor(registry))
        };
    }
}
