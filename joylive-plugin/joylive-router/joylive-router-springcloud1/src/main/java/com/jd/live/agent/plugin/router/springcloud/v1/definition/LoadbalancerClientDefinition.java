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
package com.jd.live.agent.plugin.router.springcloud.v1.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.plugin.router.springcloud.v1.condition.ConditionalOnSpringCloud1FlowControlEnabled;
import com.jd.live.agent.plugin.router.springcloud.v1.interceptor.LoadbalancerClientInterceptor;

/**
 * LoadbalancerClientDefinition
 *
 * @since 1.9.0
 */
@Extension(value = "LoadbalancerClientDefinition_v1")
@ConditionalOnSpringCloud1FlowControlEnabled
@ConditionalOnClass(LoadbalancerClientDefinition.TYPE)
public class LoadbalancerClientDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "com.netflix.loadbalancer.AbstractLoadBalancer";

    private static final String METHOD = "executeWithLoadBalancer";

    public LoadbalancerClientDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(2)),
                        // cannot LoadbalancerClientInterceptor::new, because of the classloader issue
                        () -> new LoadbalancerClientInterceptor()),
        };
    }
}
