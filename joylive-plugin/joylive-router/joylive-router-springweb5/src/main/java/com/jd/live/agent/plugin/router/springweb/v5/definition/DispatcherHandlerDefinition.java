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
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springweb.v5.condition.ConditionalOnSpringWeb5GovernanceEnabled;
import com.jd.live.agent.plugin.router.springweb.v5.interceptor.DispatcherHandlerInterceptor;

/**
 * DispatcherHandlerDefinition
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Injectable
@Extension(value = "DispatcherHandlerDefinition_v5")
@ConditionalOnSpringWeb5GovernanceEnabled
@ConditionalOnClass(DispatcherHandlerDefinition.TYPE_DISPATCHER_HANDLER)
@ConditionalOnClass(DispatcherHandlerDefinition.REACTOR_MONO)
public class DispatcherHandlerDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_DISPATCHER_HANDLER = "org.springframework.web.reactive.DispatcherHandler";

    protected static final String REACTOR_MONO = "reactor.core.publisher.Mono";

    private static final String METHOD_INVOKE_HANDLER = "invokeHandler";

    private static final String[] ARGUMENT_HANDLE = new String[]{
            "org.springframework.web.server.ServerWebExchange",
            "java.lang.Object"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public DispatcherHandlerDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_DISPATCHER_HANDLER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_INVOKE_HANDLER).
                                and(MatcherBuilder.arguments(ARGUMENT_HANDLE)),
                        () -> new DispatcherHandlerInterceptor(context)
                )
        };
    }
}
