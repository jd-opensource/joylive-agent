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
package com.jd.live.agent.plugin.router.springweb.v7.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.plugin.router.springweb.v7.condition.ConditionalOnSpringWeb7FlowControlEnabled;
import com.jd.live.agent.plugin.router.springweb.v7.interceptor.ExceptionCarryingInterceptor;

/**
 * Plugin definition for capturing servlet exceptions and carrying them back via response headers.
 * Intercepts exception handling in Spring MVC's DispatcherServlet.
 *
 * @author Axkea
 */
@Injectable
@Extension(value = "ExceptionCarryingDefinition_v7")
@ConditionalOnSpringWeb7FlowControlEnabled
@ConditionalOnClass(ExceptionCarryingDefinition.TYPE_DISPATCHER_SERVLET)
@ConditionalOnClass(ExceptionCarryingDefinition.TYPE_HTTP_SERVLET)
public class ExceptionCarryingDefinition extends PluginDefinitionAdapter {
    protected static final String TYPE_DISPATCHER_SERVLET = "org.springframework.web.servlet.DispatcherServlet";

    protected static final String METHOD = "processHandlerException";

    protected static final String TYPE_HTTP_SERVLET = "jakarta.servlet.http.HttpServlet";

    @Inject(ServiceConfig.COMPONENT_SERVICE_CONFIG)
    private ServiceConfig serviceConfig;

    public ExceptionCarryingDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_DISPATCHER_SERVLET);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD),
                        () -> new ExceptionCarryingInterceptor(serviceConfig)
                )
        };
    }
}
