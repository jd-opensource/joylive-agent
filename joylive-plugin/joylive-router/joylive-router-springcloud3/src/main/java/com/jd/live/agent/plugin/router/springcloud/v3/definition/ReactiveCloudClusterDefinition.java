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
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnReactive;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springcloud.v3.condition.ConditionalOnSpringCloud3FlowControlEnabled;
import com.jd.live.agent.plugin.router.springcloud.v3.interceptor.ReactiveCloudClusterInterceptor;

/**
 * ReactiveClusterDefinition
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "ClientClusterDefinition_v3")
@ConditionalOnSpringCloud3FlowControlEnabled
@ConditionalOnReactive
@ConditionalOnClass(ReactiveCloudClusterDefinition.TYPE_LOADBALANCED_EXCHANGE_FILTER_FUNCTION)
public class ReactiveCloudClusterDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_LOADBALANCED_EXCHANGE_FILTER_FUNCTION = "org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction";

    private static final String METHOD_FILTER = "filter";

    private static final String[] ARGUMENT_FILTER = new String[]{
            "org.springframework.web.reactive.function.client.ClientRequest",
            "org.springframework.web.reactive.function.client.ExchangeFunction"
    };

    protected static final String REACTOR_MONO = "reactor.core.publisher.Mono";

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public ReactiveCloudClusterDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE_LOADBALANCED_EXCHANGE_FILTER_FUNCTION);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_FILTER).
                                and(MatcherBuilder.arguments(ARGUMENT_FILTER)),
                        () -> new ReactiveCloudClusterInterceptor(context)
                )
        };
    }
}
