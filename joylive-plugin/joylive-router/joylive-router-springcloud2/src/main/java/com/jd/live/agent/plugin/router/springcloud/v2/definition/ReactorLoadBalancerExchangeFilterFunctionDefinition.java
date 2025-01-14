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
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springcloud.v2.condition.ConditionalOnSpringCloud2GovernanceEnabled;
import com.jd.live.agent.plugin.router.springcloud.v2.interceptor.ReactiveClusterInterceptor;

/**
 * @author: yuanjinzhong
 * @date: 2025/1/2 19:52
 * @description: When <code>spring.cloud.loadbalancer.ribbon.enabled=false </code> is configured in the application, ReactorLoadBalancerExchangeFilterFunction is automatically injected;
 * otherwise, LoadBalancerExchangeFilterFunction is injected. Note that they have an either-or relationship.
 * @see  org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction
 */
@Injectable
@Extension(value = "ReactorExchangeFilterFunctionDefinition_v2")
@ConditionalOnSpringCloud2GovernanceEnabled
@ConditionalOnClass(ReactorLoadBalancerExchangeFilterFunctionDefinition.TYPE_REACTOR_LOADBALANCER_EXCHANGE_FILTER)
public class ReactorLoadBalancerExchangeFilterFunctionDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_REACTOR_LOADBALANCER_EXCHANGE_FILTER = "org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction";

    private static final String METHOD_INTERCEPT = "filter";

    private static final String[] ARGUMENT_INTERCEPT = new String[]{
            "org.springframework.web.reactive.function.client.ClientRequest",
            "org.springframework.web.reactive.function.client.ExchangeFunction"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public ReactorLoadBalancerExchangeFilterFunctionDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_REACTOR_LOADBALANCER_EXCHANGE_FILTER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_INTERCEPT).
                                and(MatcherBuilder.arguments(ARGUMENT_INTERCEPT)),
                        () ->  new ReactiveClusterInterceptor(context)
                )
        };
    }
}