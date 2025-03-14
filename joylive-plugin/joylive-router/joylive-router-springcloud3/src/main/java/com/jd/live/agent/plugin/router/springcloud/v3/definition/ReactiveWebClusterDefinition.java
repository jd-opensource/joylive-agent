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
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.router.springcloud.v3.condition.ConditionalOnSpringWeb5RegistryEnabled;
import com.jd.live.agent.plugin.router.springcloud.v3.interceptor.ReactiveWebClusterInterceptor;

/**
 * WebClientClusterDefinition
 */
@Injectable
@Extension(value = "WebClientClusterDefinition_v5")
@ConditionalOnSpringWeb5RegistryEnabled
@ConditionalOnReactive
@ConditionalOnClass(ReactiveWebClusterDefinition.TYPE_DEFAULT_WEB_CLIENT)
public class ReactiveWebClusterDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_DEFAULT_WEB_CLIENT = "org.springframework.web.reactive.function.client.DefaultWebClient";

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    @Inject(Registry.COMPONENT_REGISTRY)
    private Registry registry;

    public ReactiveWebClusterDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_DEFAULT_WEB_CLIENT);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.isConstructor(),
                        () -> new ReactiveWebClusterInterceptor(context, registry))
        };
    }
}
