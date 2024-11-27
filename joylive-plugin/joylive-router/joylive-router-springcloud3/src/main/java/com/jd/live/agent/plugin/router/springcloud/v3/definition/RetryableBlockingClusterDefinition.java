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
package com.jd.live.agent.plugin.router.springcloud.v3.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnMissingClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springcloud.v3.interceptor.BlockingClusterInterceptor;

/**
 * BlockingRetryClusterDefinition
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "BlockingRetryClusterDefinition_v3")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_SPRING_ENABLED, matchIfMissing = true)
@ConditionalOnClass(RetryableBlockingClusterDefinition.TYPE_RETRY_LOADBALANCER_INTERCEPTOR)
@ConditionalOnClass(RetryableBlockingClusterDefinition.TYPE_RETRY_TEMPLATE)
@ConditionalOnClass(BlockingClusterDefinition.TYPE_STICKY_SESSION_SUPPLIER)
@ConditionalOnMissingClass(BlockingClusterDefinition.TYPE_HTTP_STATUS_CODE)
public class RetryableBlockingClusterDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_RETRY_LOADBALANCER_INTERCEPTOR = "org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor";

    protected static final String TYPE_RETRY_TEMPLATE = "org.springframework.retry.support.RetryTemplate";

    private static final String METHOD_INTERCEPT = "intercept";

    private static final String[] ARGUMENT_INTERCEPT = new String[]{
            "org.springframework.http.HttpRequest",
            "byte[]",
            "org.springframework.http.client.ClientHttpRequestExecution"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public RetryableBlockingClusterDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_RETRY_LOADBALANCER_INTERCEPTOR);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_INTERCEPT).
                                and(MatcherBuilder.arguments(ARGUMENT_INTERCEPT)),
                        () -> new BlockingClusterInterceptor(context)
                )
        };
    }
}
