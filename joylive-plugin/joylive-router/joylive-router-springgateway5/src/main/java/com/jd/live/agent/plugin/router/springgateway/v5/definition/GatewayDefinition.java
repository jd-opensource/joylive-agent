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
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.plugin.router.springgateway.v5.condition.ConditionalOnSpringGateway4OnlyRouteEnabled;
import com.jd.live.agent.plugin.router.springgateway.v5.interceptor.GatewayInterceptor;

/**
 * GatewayDefinition
 *
 * @since 1.0.0
 */
@Extension(value = "GatewayDefinition_v5")
@ConditionalOnSpringGateway4OnlyRouteEnabled
@ConditionalOnClass(GatewayDefinition.TYPE_FILTERING_WEB_HANDLER)
public class GatewayDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_FILTERING_WEB_HANDLER = "org.springframework.cloud.gateway.handler.FilteringWebHandler";

    private static final String METHOD_HANDLE = "handle";

    private static final String[] ARGUMENT_HANDLE = new String[]{
            "org.springframework.web.server.ServerWebExchange"
    };

    protected static final String REACTOR_MONO = "reactor.core.publisher.Mono";

    public GatewayDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_FILTERING_WEB_HANDLER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_HANDLE).
                                and(MatcherBuilder.arguments(ARGUMENT_HANDLE)),
                        () -> new GatewayInterceptor()
                )
        };
    }
}
