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
package com.jd.live.agent.plugin.router.springcloud.v2_1.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springcloud.v2_1.condition.ConditionalOnSpringCloud2FlowControlEnabled;
import com.jd.live.agent.plugin.router.springcloud.v2_1.interceptor.ReactiveCloudClusterInterceptor;

@Injectable
@Extension(value = "ReactiveCloudClusterDefinition_v2.1")
@ConditionalOnSpringCloud2FlowControlEnabled
@ConditionalOnClass(ReactiveCloudClusterDefinition.TYPE_LOADBALANCER_EXCHANGE_FILTER)
public class ReactiveCloudClusterDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_LOADBALANCER_EXCHANGE_FILTER = "org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerExchangeFilterFunction";

    protected static final String TYPE_REACTOR_LOADBALANCER_EXCHANGE_FILTER = "org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction";

    private static final String METHOD_INTERCEPT = "filter";

    private static final String[] ARGUMENT_INTERCEPT = new String[]{
            "org.springframework.web.reactive.function.client.ClientRequest",
            "org.springframework.web.reactive.function.client.ExchangeFunction"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public ReactiveCloudClusterDefinition() {
        this.matcher = () -> MatcherBuilder.in(TYPE_LOADBALANCER_EXCHANGE_FILTER, TYPE_REACTOR_LOADBALANCER_EXCHANGE_FILTER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_INTERCEPT).
                                and(MatcherBuilder.arguments(ARGUMENT_INTERCEPT)),
                        () -> new ReactiveCloudClusterInterceptor(context)
                )
        };
    }
}
