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
package com.jd.live.agent.plugin.registry.springcloud.v5.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.registry.springcloud.v5.condition.ConditionalOnSpringCloud5GovernanceEnabled;
import com.jd.live.agent.plugin.registry.springcloud.v5.interceptor.FeignClientFactoryBeanInterceptor;

/**
 * FeignClientFactoryBeanDefinition
 */
@Injectable
@Extension(value = "FeignClientFactoryBeanDefinition_v5", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnSpringCloud5GovernanceEnabled
@ConditionalOnClass(FeignClientFactoryBeanDefinition.TYPE_FEIGN_CLIENT_FACTORY_BEAN)
public class FeignClientFactoryBeanDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_FEIGN_CLIENT_FACTORY_BEAN = "org.springframework.cloud.openfeign.FeignClientFactoryBean";

    private static final String METHOD_FEIGN = "feign";

    // for spring cloud feign 2/3
    private static final String[] ARGUMENT_FEIGN3 = new String[]{
            "org.springframework.cloud.openfeign.FeignContext"
    };

    // for spring cloud feign 4
    private static final String[] ARGUMENT_FEIGN4 = new String[]{
            "org.springframework.cloud.openfeign.FeignClientFactory"
    };

    @Inject(Registry.COMPONENT_REGISTRY)
    private Registry registry;

    public FeignClientFactoryBeanDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_FEIGN_CLIENT_FACTORY_BEAN);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_FEIGN).
                                and(MatcherBuilder.arguments(ARGUMENT_FEIGN3)),
                        () -> new FeignClientFactoryBeanInterceptor(registry)),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_FEIGN).
                                and(MatcherBuilder.arguments(ARGUMENT_FEIGN4)),
                        () -> new FeignClientFactoryBeanInterceptor(registry))
        };
    }
}
