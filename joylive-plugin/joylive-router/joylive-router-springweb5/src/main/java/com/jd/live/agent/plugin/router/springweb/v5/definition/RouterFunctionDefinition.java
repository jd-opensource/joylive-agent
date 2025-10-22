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
package com.jd.live.agent.plugin.router.springweb.v5.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.parser.JsonPathParser;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springweb.v5.condition.ConditionalOnSpringWeb5GovernanceEnabled;
import com.jd.live.agent.plugin.router.springweb.v5.interceptor.RouterFunctionInterceptor;

/**
 * Plugin definition for intercepting Spring Web RouterFunction invocations.
 * Provides governance capabilities and MCP exception wrapping for functional routing.
 *
 * <p>This definition targets Spring's RouterFunction to intercept routing requests
 * via the 'route' method in functional style web endpoints.</p>
 *
 * @since 1.9.0
 */
@Injectable
@Extension(value = "RouterFunctionDefinition_v5")
@ConditionalOnSpringWeb5GovernanceEnabled
@ConditionalOnClass(RouterFunctionDefinition.TYPE)
public class RouterFunctionDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.springframework.web.servlet.function.RouterFunction";

    private static final String METHOD = "route";

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    @Inject
    private JsonPathParser parser;

    public RouterFunctionDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD), () -> new RouterFunctionInterceptor(context, parser)
                )
        };
    }
}
