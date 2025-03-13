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
package com.jd.live.agent.plugin.router.springcloud.v2.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnSpringRetry;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springcloud.v2.condition.ConditionalOnSpringCloud2GovernanceEnabled;
import com.jd.live.agent.plugin.router.springcloud.v2.interceptor.FeignCloudClusterInterceptor;

/**
 * FeignRetryableClientDefinition
 *
 * @since 1.5.0
 */
@Injectable
@Extension(value = "FeignRetryableClientDefinition_v2")
@ConditionalOnSpringCloud2GovernanceEnabled
@ConditionalOnSpringRetry
@ConditionalOnClass(FeignRetryableCloudClusterDefinition.TYPE_RETRYABLE_FEIGN_BLOCKING_LOADBALANCER_CLIENT)
public class FeignRetryableCloudClusterDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_RETRYABLE_FEIGN_BLOCKING_LOADBALANCER_CLIENT = "org.springframework.cloud.openfeign.loadbalancer.RetryableFeignBlockingLoadBalancerClient";

    private static final String METHOD_EXECUTE = "execute";

    private static final String[] ARGUMENT_FILTER = new String[]{
            "feign.Request",
            "feign.Request$Options"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public FeignRetryableCloudClusterDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_RETRYABLE_FEIGN_BLOCKING_LOADBALANCER_CLIENT);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_EXECUTE).
                                and(MatcherBuilder.arguments(ARGUMENT_FILTER)),
                        () -> new FeignCloudClusterInterceptor(context)
                )
        };
    }
}
