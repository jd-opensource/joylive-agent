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

import com.jd.live.agent.bootstrap.classloader.ResourcerType;
import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.ExtensibleDesc;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.request.HeaderProviderFactory;
import com.jd.live.agent.plugin.router.springweb.v5.condition.ConditionalOnSpringWeb5GovernanceEnabled;
import com.jd.live.agent.plugin.router.springweb.v5.interceptor.HandlerAdapterInterceptor;

/**
 * HandlerAdapterDefinition
 *
 * @since 1.1.0
 */
@Injectable
@Extension(value = "HandlerAdapterDefinition_v5")
@ConditionalOnSpringWeb5GovernanceEnabled
@ConditionalOnClass(HandlerAdapterDefinition.TYPE_HANDLER_ADAPTER)
public class HandlerAdapterDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_HANDLER_ADAPTER = "org.springframework.web.servlet.HandlerAdapter";

    private static final String METHOD_HANDLE = "handle";

    private static final String[] ARGUMENT_HANDLE = new String[]{
            "javax.servlet.http.HttpServletRequest",
            "javax.servlet.http.HttpServletResponse",
            "java.lang.Object"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    // lazy load header provider factory by ExtensibleDesc
    @Inject(loader = ResourcerType.CORE_IMPL)
    private ExtensibleDesc<HeaderProviderFactory> extensibleDesc;

    public HandlerAdapterDefinition() {
        // org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter
        //  <-- org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
        // org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter
        // org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter
        // org.springframework.web.servlet.function.support.HandlerFunctionAdapter
        this.matcher = () -> MatcherBuilder.isImplement(TYPE_HANDLER_ADAPTER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_HANDLE).
                                and(MatcherBuilder.arguments(ARGUMENT_HANDLE)),
                        () -> new HandlerAdapterInterceptor(context, extensibleDesc)
                )
        };
    }
}
