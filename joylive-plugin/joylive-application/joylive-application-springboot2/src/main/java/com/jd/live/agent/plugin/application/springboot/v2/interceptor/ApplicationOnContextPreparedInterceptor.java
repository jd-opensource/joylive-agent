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
package com.jd.live.agent.plugin.application.springboot.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.core.bootstrap.AppListener;
import com.jd.live.agent.core.bootstrap.AppListener.AppListenerAdapter;
import com.jd.live.agent.core.bootstrap.AppListenerSupervisor;
import com.jd.live.agent.core.mcp.handler.McpHandler;
import com.jd.live.agent.core.mcp.version.McpVersion;
import com.jd.live.agent.core.parser.ObjectConverter;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.McpConfig;
import com.jd.live.agent.plugin.application.springboot.v2.context.SpringAppContext;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.reactive.ReactiveMcpController;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.web.jakarta.JakartaWebMcpController;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.web.javax.JavaxWebMcpController;
import com.jd.live.agent.plugin.application.springboot.v2.util.AppLifecycle;
import com.jd.live.agent.plugin.application.springboot.v2.util.SpringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

/**
 * Interceptor that handles context prepared events by notifying registered listeners
 */
public class ApplicationOnContextPreparedInterceptor extends InterceptorAdaptor {

    private final GovernanceConfig config;
    private final AppListenerSupervisor supervisor;
    private final Map<String, McpHandler> handlers;
    private final ObjectConverter converter;
    private final ObjectParser parser;
    private final Map<String, McpVersion> versions;
    private final McpVersion defaultVersion;

    public ApplicationOnContextPreparedInterceptor(AppListenerSupervisor supervisor,
                                                   GovernanceConfig config,
                                                   Map<String, McpHandler> handlers,
                                                   ObjectConverter converter,
                                                   ObjectParser parser,
                                                   Map<String, McpVersion> versions,
                                                   McpVersion defaultVersion) {
        this.supervisor = supervisor;
        this.config = config;
        this.handlers = handlers;
        this.converter = converter;
        this.parser = parser;
        this.versions = versions;
        this.defaultVersion = defaultVersion;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        AppLifecycle.contextPrepared(() -> {
            McpConfig mcpConfig = config.getMcpConfig();
            if (mcpConfig.isEnabled()) {
                AppListener listener = new ContextPreparedListener();
                supervisor.addFirst(listener);
                try {
                    supervisor.onContextPrepared(new SpringAppContext(ctx.getArgument(0)));
                } finally {
                    supervisor.remove(listener);
                }
            } else {
                supervisor.onContextPrepared(new SpringAppContext(ctx.getArgument(0)));
            }
        });
    }

    private class ContextPreparedListener extends AppListenerAdapter {

        @Override
        public void onContextPrepared(AppContext context) {
            if (!(context instanceof SpringAppContext)) {
                return;
            }
            ConfigurableApplicationContext ctx = ((SpringAppContext) context).getContext();
            ConfigurableEnvironment environment = ctx.getEnvironment();
            ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
            if (beanFactory instanceof BeanDefinitionRegistry) {
                Class<?> mcpType = null;
                String beanName = null;
                if (SpringUtils.isWeb(environment)) {
                    if (SpringUtils.isJavaxServlet()) {
                        mcpType = JavaxWebMcpController.class;
                        beanName = JavaxWebMcpController.NAME;
                    } else {
                        mcpType = JakartaWebMcpController.class;
                        beanName = JakartaWebMcpController.NAME;
                    }
                } else if (SpringUtils.isWebFlux(environment)) {
                    mcpType = ReactiveMcpController.class;
                    beanName = ReactiveMcpController.NAME;
                }
                if (mcpType != null) {
                    BeanDefinition definition = BeanDefinitionBuilder
                            .genericBeanDefinition(mcpType)
                            .addPropertyValue("objectConverter", converter)
                            .addPropertyValue("objectParser", parser)
                            .addPropertyValue("handlers", handlers)
                            .addPropertyValue("config", config)
                            .addPropertyValue("versions", versions)
                            .addPropertyValue("defaultVersion", defaultVersion)
                            .getBeanDefinition();
                    BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
                    registry.registerBeanDefinition(beanName, definition);
                }
            }
        }
    }
}
