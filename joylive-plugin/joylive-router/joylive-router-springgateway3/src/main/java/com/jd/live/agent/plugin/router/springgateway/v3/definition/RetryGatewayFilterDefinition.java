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
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.router.springgateway.v3.interceptor.RetryGatewayFilterInterceptor;

/**
 * RetryGatewayFilterDefinition
 *
 * @since 1.0.0
 */
@Extension(value = "RetryGatewayFilterDefinition_v3")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_SPRING_GATEWAY_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_SPRING_ENABLED, matchIfMissing = true)
@ConditionalOnClass(RetryGatewayFilterDefinition.TYPE_RETRY_GATEWAY_FILTER_FACTORY)
public class RetryGatewayFilterDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_RETRY_GATEWAY_FILTER_FACTORY = "org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory";

    private static final String TYPE_GATEWAY_FILTER = "org.springframework.cloud.gateway.filter.GatewayFilter";

    private static final String METHOD_FILTER = "filter";

    private static final String[] ARGUMENT_FILTER = new String[]{
            "org.springframework.web.server.ServerWebExchange",
            "org.springframework.cloud.gateway.filter.GatewayFilterChain"
    };

    public RetryGatewayFilterDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE_GATEWAY_FILTER)
                .and(MatcherBuilder.startWith(TYPE_RETRY_GATEWAY_FILTER_FACTORY));
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_FILTER)
                                .and(MatcherBuilder.arguments(ARGUMENT_FILTER)),
                        new RetryGatewayFilterInterceptor()
                )
        };
    }
}
