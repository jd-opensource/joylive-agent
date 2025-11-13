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
package com.jd.live.agent.plugin.router.springweb.v6.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnReactive;
import com.jd.live.agent.plugin.router.springweb.v6.condition.ConditionalOnSpringWeb6GovernanceEnabled;
import com.jd.live.agent.plugin.router.springweb.v6.interceptor.ReactiveRouterFunctionBuilderInterceptor;

/**
 * Plugin for intercepting Spring Web RouterFunctionBuilder's build method.
 * <p>
 * This plugin captures error handlers configured in RouterFunctions and caches them
 * for subsequent processing. It enables access to the original exception handling
 * logic when the RouterFunction is wrapped by custom logic.
 * <p>
 * The interceptor is activated only when Spring Web 5 Governance is enabled and
 * the RouterFunctionBuilder class is available on the classpath.
 *
 * @since 1.9.0
 */
@Injectable
@Extension(value = "RouterFunctionBuilderDefinition_v6")
@ConditionalOnSpringWeb6GovernanceEnabled
@ConditionalOnReactive
@ConditionalOnClass(ReactiveRouterFunctionBuilderDefinition.TYPE)
public class ReactiveRouterFunctionBuilderDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.springframework.web.reactive.function.server.RouterFunctionBuilder";

    private static final String METHOD = "build";

    public ReactiveRouterFunctionBuilderDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD), () -> new ReactiveRouterFunctionBuilderInterceptor()
                )
        };
    }
}
