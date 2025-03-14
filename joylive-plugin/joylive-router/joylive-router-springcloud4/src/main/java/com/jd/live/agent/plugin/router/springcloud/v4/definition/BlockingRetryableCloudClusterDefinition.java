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
package com.jd.live.agent.plugin.router.springcloud.v4.definition;

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
import com.jd.live.agent.plugin.router.springcloud.v4.condition.ConditionalOnSpringCloud4FlowControlEnabled;
import com.jd.live.agent.plugin.router.springcloud.v4.interceptor.BlockingCloudClusterInterceptor;

/**
 * BlockingRetryClusterDefinition
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "BlockingRetryClusterDefinition_v4")
@ConditionalOnSpringCloud4FlowControlEnabled
@ConditionalOnSpringRetry
@ConditionalOnClass(BlockingRetryableCloudClusterDefinition.TYPE_RETRY_LOADBALANCER_INTERCEPTOR)
public class BlockingRetryableCloudClusterDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_RETRY_LOADBALANCER_INTERCEPTOR = "org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor";

    private static final String METHOD_INTERCEPT = "intercept";

    private static final String[] ARGUMENT_INTERCEPT = new String[]{
            "org.springframework.http.HttpRequest",
            "byte[]",
            "org.springframework.http.client.ClientHttpRequestExecution"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public BlockingRetryableCloudClusterDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_RETRY_LOADBALANCER_INTERCEPTOR);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_INTERCEPT).
                                and(MatcherBuilder.arguments(ARGUMENT_INTERCEPT)),
                        () -> new BlockingCloudClusterInterceptor(context)
                )
        };
    }
}
