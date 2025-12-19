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
package com.jd.live.agent.plugin.application.springboot.mcp;

import com.jd.live.agent.core.bootstrap.AppBeanDefinition;
import com.jd.live.agent.core.bootstrap.AppBooter;
import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.core.bootstrap.AppListener;
import com.jd.live.agent.core.bootstrap.AppListener.AppListenerAdapter;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.mcp.handler.McpHandler;
import com.jd.live.agent.core.mcp.version.McpVersion;
import com.jd.live.agent.core.openapi.spec.v3.OpenApiFactory;
import com.jd.live.agent.core.parser.ObjectConverter;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.application.springboot.mcp.web.jakarta.JakartaWebMcpController;
import com.jd.live.agent.plugin.application.springboot.mcp.web.javax.JavaxWebMcpController;
import com.jd.live.agent.plugin.application.springboot.mcp.web.reactive.ReactiveMcpController;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;
import java.util.Map;

@Extension(value = "McpAppRegister", order = AppListener.ORDER_MCP)
@ConditionalOnProperty(GovernanceConfig.CONFIG_MCP_ENABLED)
@ConditionalOnClass("org.springframework.context.ConfigurableApplicationContext")
@Injectable
public class McpRegister extends AppListenerAdapter implements AppBooter {

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    @Inject
    private Map<String, McpHandler> handlers;

    @Inject
    private ObjectConverter converter;

    @Inject(ObjectParser.JSON)
    private ObjectParser parser;

    @Inject
    private Map<String, McpVersion> versions;

    @Override
    public void onContextPrepared(AppContext ctx) {
        Object delegate = ctx.unwrap();
        if (!(delegate instanceof ConfigurableApplicationContext)) {
            return;
        }
        ConfigurableApplicationContext ac = (ConfigurableApplicationContext) delegate;
        ConfigurableListableBeanFactory beanFactory = ac.getBeanFactory();
        if (!(beanFactory instanceof BeanDefinitionRegistry)) {
            return;
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put("context", context);
        properties.put("objectConverter", converter);
        properties.put("objectParser", parser);
        properties.put("handlers", handlers);
        properties.put("config", context.getGovernanceConfig());
        properties.put("versions", versions);
        switch (ctx.getWebType()) {
            case WEB_SERVLET_JAVAX:
                ctx.register(new AppBeanDefinition(JavaxWebMcpController.NAME, JavaxWebMcpController.class, properties));
                break;
            case WEB_SERVLET_JAKARTA:
                ctx.register(new AppBeanDefinition(JakartaWebMcpController.NAME, JakartaWebMcpController.class, properties));
                break;
            case WEB_REACTIVE:
                ctx.register(new AppBeanDefinition(ReactiveMcpController.NAME, ReactiveMcpController.class, properties));
                break;
        }
    }

    @Override
    public void onStarted(AppContext context) {
        OpenApiFactory apiFactory = OpenApiFactory.INSTANCE_REF.get();
        if (apiFactory != null) {
            ClassLoader classLoader = context.unwrap().getClass().getClassLoader();
            switch (context.getWebType()) {
                case WEB_SERVLET_JAVAX:
                    apiFactory.addHiddenController(JavaxWebMcpController.class, classLoader);
                    break;
                case WEB_SERVLET_JAKARTA:
                    apiFactory.addHiddenController(JakartaWebMcpController.class, classLoader);
                    break;
                case WEB_REACTIVE:
                    apiFactory.addHiddenController(ReactiveMcpController.class, classLoader);
                    break;
            }
        }
    }
}
