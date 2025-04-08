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
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnReactive;
import com.jd.live.agent.plugin.router.springcloud.v4.condition.ConditionalOnSpringCloud4GovernanceEnabled;
import com.jd.live.agent.plugin.router.springcloud.v4.interceptor.ReactorLoadBalancerInterceptor;

/**
 * ReactorLoadBalancerDefinition
 *
 * @since 1.0.0
 */
@Extension(value = "ReactorLoadBalancerDefinition_v4")
@ConditionalOnSpringCloud4GovernanceEnabled
@ConditionalOnReactive
@ConditionalOnClass(ReactorLoadBalancerDefinition.TYPE_REACTOR_LOAD_BALANCER)
public class ReactorLoadBalancerDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_REACTOR_LOAD_BALANCER = "org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer";

    private static final String METHOD_CHOOSE = "choose";

    private static final String[] ARGUMENTS_CHOOSE = new String[]{
            "org.springframework.cloud.client.loadbalancer.Request"
    };

    public ReactorLoadBalancerDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE_REACTOR_LOAD_BALANCER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_CHOOSE).
                                and(MatcherBuilder.arguments(ARGUMENTS_CHOOSE)),
                        () -> new ReactorLoadBalancerInterceptor())
        };
    }
}
