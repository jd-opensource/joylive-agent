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
import com.jd.live.agent.core.extension.annotation.*;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springgateway.v3.config.GatewayConfig;
import com.jd.live.agent.plugin.router.springgateway.v3.interceptor.GatewayClusterInterceptor;

/**
 * ReactiveLoadBalancerClientFilter
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "GatewayClusterDefinition_v3")
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(name = {
                GovernanceConfig.CONFIG_LIVE_ENABLED,
                GovernanceConfig.CONFIG_LANE_ENABLED,
                GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED
        }, relation = ConditionalRelation.OR),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_SPRING_GATEWAY_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_SPRING_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.AND)
@ConditionalOnClass(GatewayClusterDefinition.TYPE_REACTIVE_LOADBALANCER_CLIENT_FILTER)
@ConditionalOnClass(GatewayClusterDefinition.REACTOR_MONO)
@ConditionalOnMissingClass(FilteringWebHandlerDefinition.TYPE_HTTP_STATUS_CODE)
public class GatewayClusterDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_REACTIVE_LOADBALANCER_CLIENT_FILTER = "org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter";

    private static final String METHOD_FILTER = "filter";

    private static final String[] ARGUMENT_FILTER = new String[]{
            "org.springframework.web.server.ServerWebExchange",
            "org.springframework.cloud.gateway.filter.GatewayFilterChain"
    };

    protected static final String REACTOR_MONO = "reactor.core.publisher.Mono";

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    @Config(GatewayConfig.CONFIG_SPRING_GATEWAY_PREFIX)
    private GatewayConfig config;

    public GatewayClusterDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_REACTIVE_LOADBALANCER_CLIENT_FILTER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_FILTER).
                                and(MatcherBuilder.arguments(ARGUMENT_FILTER)),
                        () -> new GatewayClusterInterceptor(context, config)
                )
        };
    }
}
