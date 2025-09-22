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
package com.jd.live.agent.plugin.router.springcloud.v5.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnReactive;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springcloud.v5.condition.ConditionalOnSpringCloud5FlowControlEnabled;
import com.jd.live.agent.plugin.router.springcloud.v5.interceptor.ReactiveCloudClientInterceptor;

/**
 * ReactiveCloudClusterDefinition
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "ReactiveCloudClusterDefinition_v5")
@ConditionalOnSpringCloud5FlowControlEnabled
@ConditionalOnReactive
@ConditionalOnClass(ReactiveCloudClientDefinition.TYPE_LOADBALANCER_EXCHANGE_FILTER_FUNCTION)
public class ReactiveCloudClientDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_LOADBALANCER_EXCHANGE_FILTER_FUNCTION = "org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction";

    private static final String METHOD_FILTER = "filter";

    private static final String[] ARGUMENT_FILTER = new String[]{
            "org.springframework.web.reactive.function.client.ClientRequest",
            "org.springframework.web.reactive.function.client.ExchangeFunction"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public ReactiveCloudClientDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE_LOADBALANCER_EXCHANGE_FILTER_FUNCTION);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_FILTER).
                                and(MatcherBuilder.arguments(ARGUMENT_FILTER)),
                        () -> new ReactiveCloudClientInterceptor(context)
                )
        };
    }
}
