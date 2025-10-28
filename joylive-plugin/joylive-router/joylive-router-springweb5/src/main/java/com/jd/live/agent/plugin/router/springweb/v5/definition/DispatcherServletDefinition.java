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
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.plugin.router.springweb.v5.condition.ConditionalOnSpringWeb5FlowControlEnabled;
import com.jd.live.agent.plugin.router.springweb.v5.interceptor.DispatcherServletExceptionInterceptor;

/**
 * Plugin definition for intercepting DispatcherServlet exceptions for processing.
 *
 * <p>This plugin targets Spring MVC DispatcherServlet and intercepts the
 * processHandlerException method to handle and process exceptions that occur
 * during request processing in servlet-based web applications.
 *
 * @author Axkea
 */
@Injectable
@Extension(value = "ExceptionCarryingDefinition_v5")
@ConditionalOnSpringWeb5FlowControlEnabled
@ConditionalOnClass(DispatcherServletDefinition.TYPE_DISPATCHER_SERVLET)
@ConditionalOnClass(DispatcherServletDefinition.TYPE_HTTP_SERVLET)
public class DispatcherServletDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_DISPATCHER_SERVLET = "org.springframework.web.servlet.DispatcherServlet";

    protected static final String TYPE_HTTP_SERVLET = "javax.servlet.http.HttpServlet";

    protected static final String METHOD = "processHandlerException";

    @Inject(ServiceConfig.COMPONENT_SERVICE_CONFIG)
    private ServiceConfig serviceConfig;

    public DispatcherServletDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_DISPATCHER_SERVLET);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD),
                        () -> new DispatcherServletExceptionInterceptor(serviceConfig)
                )
        };
    }
}
