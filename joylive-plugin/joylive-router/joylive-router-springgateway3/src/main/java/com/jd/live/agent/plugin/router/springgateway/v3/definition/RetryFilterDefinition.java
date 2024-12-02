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
import com.jd.live.agent.core.extension.annotation.ConditionalOnMissingClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.router.springgateway.v3.interceptor.RetryFilterInterceptor;

/**
 * RetryFilterDefinition
 *
 * @since 1.6.0
 */
@Extension(value = "RetryFilterDefinition_v3")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_SPRING_GATEWAY_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_SPRING_ENABLED, matchIfMissing = true)
@ConditionalOnClass(RetryFilterDefinition.TYPE_RETRY_GATEWAY_FILTER_FACTORY_$1)
@ConditionalOnClass(RetryFilterDefinition.REACTOR_MONO)
@ConditionalOnClass(GatewayClusterDefinition.TYPE_STICKY_SESSION_SUPPLIER)
@ConditionalOnMissingClass(RetryFilterDefinition.TYPE_HTTP_STATUS_CODE)
@Injectable
public class RetryFilterDefinition extends PluginDefinitionAdapter {

    // Order 2
    protected static final String TYPE_RETRY_GATEWAY_FILTER_FACTORY_$1 = "org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory$1";
    // spring gateway 4
    protected static final String TYPE_HTTP_STATUS_CODE = "org.springframework.http.HttpStatusCode";

    private static final String METHOD_HANDLE = "filter";

    private static final String[] ARGUMENT_HANDLE = new String[]{
            "org.springframework.web.server.ServerWebExchange",
            "org.springframework.cloud.gateway.filter.GatewayFilterChain"
    };

    protected static final String REACTOR_MONO = "reactor.core.publisher.Mono";

    public RetryFilterDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_RETRY_GATEWAY_FILTER_FACTORY_$1);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_HANDLE).
                                and(MatcherBuilder.arguments(ARGUMENT_HANDLE)),
                        RetryFilterInterceptor::new
                )
        };
    }
}
